package com.sergio.ivillager;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sergio.ivillager.NPCVillager.CustomEntity;
import org.apache.logging.log4j.Logger;


public class NPCVillagerManager {
    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerManager.class);

    private static NPCVillagerManager instance;

    private Map<Integer, VillagerData> villagersData;
    private String ssoToken;
    private String accessToken;
    private String accessKey;

    private NPCVillagerManager() {
        this.villagersData = new HashMap<>();
    }

    public static NPCVillagerManager getInstance() {
        if (instance == null) {
            instance = new NPCVillagerManager();
        }
        return instance;
    }

    public void addVillager(CustomEntity villager) {
        int id = villager.getId();
        if (!this.villagersData.containsKey(id)) {
            this.villagersData.put(id, new VillagerData(id, villager));
            LOGGER.warn("adding new NPC villager entity:");
            LOGGER.warn(villager);
        }
    }

    public VillagerData getVillagerData(int id) {
        if (this.villagersData == null) {
            return null;
        }
        return this.villagersData.get(id);
    }

    public Boolean isVillagerTalkingtoPlayer(int id){
        if (getVillagerData(id) != null) {
            return getVillagerData(id).getEntity().getIsTalkingToPlayer() != null;
        }
        return false;
    }

    public void updateVillagers(PlayerEntity player) {
        Vector3d playerPos = player.getPosition(0.5f);
        Vector3d playerLook = player.getViewVector(0.5f);

        for (VillagerData data : this.villagersData.values()) {
            CustomEntity villager = data.getEntity();
            if (villager != null) {
                Vector3d villagerPos = villager.getPosition(0.5f);
                Vector3d playerToVillager = villagerPos.subtract(playerPos);
                double distance = playerToVillager.length();
                if (villager.getIsTalkingToPlayer() != null && villager.getIsTalkingToPlayer().getId() == player.getId()) {
                    if (distance > 6 || playerLook.dot(playerToVillager) <= 0) {
                        LOGGER.warn(String.format("villager %s no longer talk to player %s",
                                villager.getCustomName().getString(),
                                player.getName().getString()));
                        villager.setIsTalkingToPlayer(null);
                        villager.goalSelector.enableControlFlag(Goal.Flag.MOVE);
                    }
                }
            }
        }
    }

    public ArrayList<CustomEntity> getNeareFacedVillager(PlayerEntity player) {
        Vector3d playerPos = player.getPosition(0.5f);
        Vector3d playerLook = player.getViewVector(0.5f);

        ArrayList<CustomEntity> nearCustomVillagerList = new ArrayList<>();

        for (VillagerData data : this.villagersData.values()) {
            CustomEntity villager = data.getEntity();
            if (villager != null) {
                Vector3d villagerPos = villager.getPosition(0.5f);
                Vector3d playerToVillager = villagerPos.subtract(playerPos);
                double distance = playerToVillager.length();
                if (distance < 6 && playerLook.dot(playerToVillager) > 0) {
                    nearCustomVillagerList.add(villager);
                }
            }
        }
        LOGGER.warn(String.format("%d villagers in manager, %d in interactive range",
                this.villagersData.size(),nearCustomVillagerList.size()));

        return nearCustomVillagerList;
    }

    public String getSsoToken() {
        return ssoToken;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public Boolean isVerified(){
        if (this.ssoToken == null || this.accessKey == null || this.accessToken == null) {
            return false;
        }
        return true;
    }
    public static class VillagerData {
        private int id;
        private CustomEntity entity;

        public VillagerData(int id, CustomEntity entity) {
            this.id = id;
            this.entity = entity;
        }

        public int getId() {
            return id;
        }

        public CustomEntity getEntity() {
            return entity;
        }
    }
}
