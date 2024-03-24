//package com.openclassrooms.tourguide.service;
//
//import com.openclassrooms.tourguide.user.User;
//import com.openclassrooms.tourguide.user.UserReward;
//import gpsUtil.location.Attraction;
//import gpsUtil.location.VisitedLocation;
//import rewardCentral.RewardCentral;
//
//import java.util.List;
//
//public class UserRewardRunnable implements Runnable{
//
//    private User user;
//
//    private RewardsService service;
//
//    private Attraction attraction;
//
//
//    public UserRewardRunnable(User user, RewardsService service, Attraction attraction) {
//        this.user = user;
//        this.service = service;
//        this.attraction = attraction;
//    }
//
////    @Override
////    public void run() {
////        for (VisitedLocation visitedLocation : user.getVisitedLocations()) {
////                if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
////                    if(service.nearAttraction(visitedLocation, attraction)) {
////                        user.addUserReward(new UserReward(visitedLocation, attraction, service.getRewardPoints(attraction, user)));
////                    }
////                }
////
////        }
////    }
//}
