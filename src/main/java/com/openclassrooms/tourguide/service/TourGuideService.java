package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.Attraction;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;

	boolean testMode = true;

	ExecutorService executorService = Executors.newFixedThreadPool(900);


	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this, testMode);

		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) throws ExecutionException, InterruptedException {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user);
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}


	public void trackAllUserLocation() {
		List<User> allUsers = getAllUsers();
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (User user : allUsers) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> trackUserLocation(user), executorService);
			futures.add(future);
		}

		futures.forEach(CompletableFuture::join);

	}



	/**
	 * Add the current location to the user and update the rewards with the new location
	 *
	 * @param user to track location
	 * @return the current location
	 */
	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		logger.info("Adding the current location to the user "+user.getUserName());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}


	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) throws ExecutionException, InterruptedException {
		List<Attraction> nearbyAttractions = new ArrayList<>();
		List<Double> distanceList = new ArrayList<>();

		// get the distance of all the attractions
		gpsUtil.getAttractions().forEach(
				e-> {
					double distance = rewardsService.getDistance(e, visitedLocation.location);
					distanceList.add(distance);
				}
		);


		// sort by order
		Collections.sort(distanceList);

		// get only the first 5 attractions

		Iterator<Double> iterator = distanceList.iterator();
		int count = 0;

		while(iterator.hasNext()) {
			iterator.next();
			if(count > 4){
				iterator.remove();
			}
			count++;
		}

		// get the attractions of all those distances
		distanceList.forEach(
				distance -> {
					gpsUtil.getAttractions().forEach(
							attraction -> {
								if(rewardsService.getDistance(attraction, visitedLocation.location) == distance){
									Attraction attractionDTO  = new Attraction();
									attractionDTO.setDistance(distance);
									attractionDTO.setLatAttraction(attraction.latitude);
									attractionDTO.setLongAttraction(attraction.longitude);
									attractionDTO.setNameAttraction(attraction.attractionName);
									attractionDTO.setLatUser(visitedLocation.location.latitude);
									attractionDTO.setLongUser(visitedLocation.location.longitude);
									attractionDTO.setRewardPoint(rewardsService.getRewardPoints(attraction, visitedLocation.userId));
									nearbyAttractions.add(attractionDTO);
								}
							}
					);
				}
		);



		return nearbyAttractions;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
