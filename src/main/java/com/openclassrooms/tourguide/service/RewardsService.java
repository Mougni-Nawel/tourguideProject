package com.openclassrooms.tourguide.service;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	ExecutorService executorService = Executors.newFixedThreadPool(900);

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}


	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = new CopyOnWriteArrayList<>(gpsUtil.getAttractions());
		List<UserReward> existingRewards = new CopyOnWriteArrayList<>(user.getUserRewards());
		attractions.removeIf(attraction -> existingRewards.stream()
				.anyMatch(reward -> reward.attraction.attractionName.equals(attraction.attractionName)));

		Set<String> alreadyRewardedAttractionName = new HashSet<>();
		for (UserReward alreadyRewarded : user.getUserRewards().stream().toList()) {
			alreadyRewardedAttractionName.add(alreadyRewarded.attraction.attractionName);
		}

		for (VisitedLocation visitedLocation : userLocations) {
			for (Attraction attraction : attractions) {
				if (nearAttraction(visitedLocation, attraction)) {
					UserReward reward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user.getUserId()));
					if (!alreadyRewardedAttractionName.contains(reward.attraction.attractionName)) {
						user.addUserReward(reward);
						alreadyRewardedAttractionName.add(reward.attraction.attractionName);
					}
				}
			}
		}
	}


	public void calculateRewardsForAllUsers(List<User> users) {
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		users.forEach(user -> futures.add(CompletableFuture.runAsync(() -> calculateRewards(user), executorService)));

		futures.forEach(CompletableFuture::join);
	}


	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	public int getRewardPoints(Attraction attraction, UUID user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user);
	}

	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

}