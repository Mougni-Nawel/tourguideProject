package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
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

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	ExecutorService executorService = Executors.newFixedThreadPool(100);

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

//	public void calculateRewards(User user) {
//		List<VisitedLocation> userLocations = user.getVisitedLocations();
//		List<Attraction> attractions = new ArrayList<>(gpsUtil.getAttractions());
//
//		List<UserReward> newRewards = new ArrayList<>();
//
//
//
//		List<VisitedLocation> userLocationsCopy = new ArrayList<>(userLocations);
//		for (VisitedLocation visitedLocation : userLocationsCopy) {
//			executorService.execute(() -> {
//				for (Attraction attraction : attractions) {
//					if (!user.getUserRewards().stream().anyMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))
//							&& nearAttraction(visitedLocation, attraction)) {
//						System.out.println("IN : "+newRewards.size());
//
//						newRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					}
//				}
//
//
//
//				for(UserReward userReward: newRewards){
//					user.addUserReward(userReward);
//				}
//
//			});
//		}
//
//
//		System.out.println("USER l: "+user.getUserRewards());
//
//
//	}

	public void calculateRewards(User user) {
		CopyOnWriteArrayList<Attraction> attractions = new CopyOnWriteArrayList<>(gpsUtil.getAttractions());

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		attractions.stream().forEach((a) -> {
				UserRewardRunnable runnable = new UserRewardRunnable(user, this, a);

				CompletableFuture<Void> future = CompletableFuture.runAsync(runnable, executorService);

				futures.add(future);
			}

		);



		// Wait for all CompletableFuture tasks to complete
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));


		// Chain a callback to execute after all tasks are completed
		allOf.thenRun(() -> {
			// This code block executes when all tasks are completed
			System.out.println("All tasks completed");
			System.out.println("USER : " + user.getUserRewards().size());
		}).exceptionally(ex -> {
			// Handle exceptions
			ex.printStackTrace();
			return null;
		}).join();
	}


//	public void getUserRewards(List<VisitedLocation> userLocations, List<Attraction> attractions, List<UserReward> newRewards, User user){
//		for (VisitedLocation visitedLocation : userLocations) {
//
//			for (Attraction attraction : attractions) {
//				if (!user.getUserRewards().stream().anyMatch(userReward -> userReward.attraction.attractionName.equals(attraction.attractionName))
//						&& nearAttraction(visitedLocation, attraction)) {
//					user.addUserReward(new UserReward(visitedLocation, attraction, rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId())));
//				}
//			}
//
//			//System.out.println(newRewards.size());
//
//
//			//});
//		}
//
//		//return newRewards;
//	}



//	public void calculateRewards(User user) {
//		User finalUser = user;
//		List<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());
//		List<Attraction> attractions = new ArrayList<>(gpsUtil.getAttractions());
//
//		List<CompletableFuture<List<UserReward>>> futures = new ArrayList<>();
//		List<UserReward> newRewards = new ArrayList<>();
//
//
//		for (VisitedLocation visitedLocation : userLocations) {
//			for (Attraction attraction : attractions) {
//				if (!user.getUserRewards().stream().anyMatch(userReward -> userReward.attraction.attractionName.equals(attraction.attractionName))
//						&& nearAttraction(visitedLocation, attraction)) {
//					newRewards.add(new UserReward(visitedLocation, attraction, this.getRewardPoints(attraction, user)));
//				}
//			}
//		}
//
//		for (UserReward userReward : newRewards) {
//			user.addUserReward(userReward);
//		}
//
////		CompletableFuture<List<UserReward>> future = CompletableFuture.supplyAsync(() -> getUserRewards(userLocations, attractions, newRewards, user));
////		future.thenApplyAsync(result -> {
////			System.out.println("REWARD : "+result);
////			result.forEach((r) -> user.addUserReward(r));
////			return null;
////		});
//
//		//futures.add(future);
//
//
//		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//
//		allOf.thenRun(() -> {
//			System.out.println("All tasks completed");
//			System.out.println("USER l: " + user.getUserRewards());
//		}).exceptionally(ex -> {
//			// Handle exceptions
//			ex.printStackTrace();
//			return null;
//		});
//	}
//
//
//	public List<UserReward> getUserRewards(List<VisitedLocation> userLocations, List<Attraction> attractions, List<UserReward> newRewards, User user){
//
//		System.out.println("R : "+newRewards);
//		return newRewards;
//	}
//
//	public void listUser(List<UserReward> newRewards, User user){
//		for (UserReward userReward : newRewards) {
//			//System.out.println("LOL"+userReward);
//			user.addUserReward(userReward);
//		}
//	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
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
