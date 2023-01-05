package com.sergio.ivillager;

import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;


public class NPCVillagerManager {
    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerManager.class);

    private static NPCVillagerManager instance;

    private Map<Integer, VillagerData> villagersData;
    private Map<String, BlockPos> villagesList;

    // Universal ssotoken
    private String ssoToken = null;
    private Map<UUID, String> accessToken = new HashMap<UUID, String>();
    private Map<UUID, String> accessKey = new HashMap<UUID, String>();

    private String accessToken_default = "";
    private String accessKey_default = "";

    private String openAIAPIKey;

    private NPCVillagerManager() {
        this.villagesList = new HashMap<>();
        this.villagersData = new HashMap<>();
    }

    public static NPCVillagerManager getInstance() {
        if (instance == null) {
            instance = new NPCVillagerManager();
        }
        return instance;
    }

    public void addVillager(NPCVillagerEntity villager) {
        int id = villager.getId();
        if (!this.villagersData.containsKey(id)) {
            this.villagersData.put(id, new VillagerData(id, villager));

            if (villager.getCustomVillagename().equals("")) {
                String n0 = isEntityLocatedAtVillagesWithName(villager);
                if (n0 != null) {
                    villager.setCustomVillagename(n0);
                } else {
                    villager.setCustomVillagename("Noman's Land");
                }
            } else {
                setVillageNameWithVillager(villager);
            }

            if (villager.getCustomNodeId().equals("")) {
                villager.generateIntelligence();
            }

            LOGGER.warn(String.format("[SERVER] Adding new NPC villager entity:[%s] living in " +
                            "Village: %s",
                    villager.getStringUUID(), villager.getCustomVillagename()));
            LOGGER.warn(villager);
        }
    }

    public List<NPCVillagerEntity> findVillagersAtSameVillage(NPCVillagerEntity entity) {
        String villageName = entity.getCustomVillagename();
        List<NPCVillagerEntity> r0 = new ArrayList<>();
        for (VillagerData data:this.villagersData.values()) {
            if (data.getEntity().getCustomVillagename().equals(villageName) && !data.getEntity().getName().getString().equals("[Awakening...]")){
                r0.add(data.getEntity());
            }
        }
        return r0;
    }

    public List<NPCVillagerEntity> findVillagersAtSameVillage(String villageName) {
        List<NPCVillagerEntity> r0 = new ArrayList<>();
        for (VillagerData data:this.villagersData.values()) {
            if (data.getEntity().getCustomVillagename().equals(villageName)){
                r0.add(data.getEntity());
            }
        }
        return r0;
    }

    public String isEntityLocatedAtVillagesWithName(NPCVillagerEntity entity){
        return this.isEntityLocatedAtVillagesWithName(entity, entity.blockPosition());
    }

    public String isEntityLocatedAtVillagesWithName(NPCVillagerEntity entity, BlockPos blockPos){
        StructureStart<?> structureAt =
                ((ServerWorld) entity.level).structureFeatureManager().getStructureAt(blockPos, true,
                        Structure.VILLAGE);
        if (!structureAt.isValid()) {
            return null;
        }

        BlockPos structureEntityAt = structureAt.getLocatePos();
        for (String name : this.villagesList.keySet()) {
            if (this.villagesList.get(name).closerThan(structureEntityAt, 5.0)) {
                return name;
            }
        }

        String n0 = Utils.randomVillageNameExclude(new ArrayList<>(this.villagesList.keySet()));
        this.villagesList.put(n0, structureEntityAt);
        return n0;
    }

    public void setVillageNameWithVillager(NPCVillagerEntity entity){
        String villageName = entity.getCustomVillagename();
        if (this.villagesList.containsKey(villageName)) {
            return;
        }

        StructureStart<?> structureAt =
                ((ServerWorld) entity.level).structureFeatureManager().getStructureAt(entity.blockPosition(),
                        true,
                        Structure.VILLAGE);
        if (!structureAt.isValid()) {
            return;
        }
        BlockPos structureEntityAt = structureAt.getLocatePos();
        this.villagesList.put(villageName, structureEntityAt);

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
//        Vector3d playerPos = player.getPosition(0.5f);
        Vector3d playerPos = player.position();
        Vector3d playerLook = player.getViewVector(0.5f);

        for (VillagerData data : this.villagersData.values()) {
            NPCVillagerEntity villager = data.getEntity();
            if (villager != null) {
//                Vector3d villagerPos = villager.getPosition(0.5f);
                Vector3d villagerPos = villager.position();
                Vector3d playerToVillager = villagerPos.subtract(playerPos);
                double distance = playerToVillager.length();
                if (villager.getIsTalkingToPlayer() != null && villager.getIsTalkingToPlayer().getId() == player.getId()) {
                    if (distance > Utils.CLOSEST_DISTANCE || playerLook.dot(playerToVillager) <= 0) {
                        LOGGER.warn(String.format("[SERVER] Villager %s [%s] no longer talk to " +
                                        "player " +
                                        "%s",
                                villager.getCustomName().getString(),villager.getStringUUID(),
                                player.getName().getString()));
                        villager.setIsTalkingToPlayer(null);
                        if (!villager.getControlWalkIsWaitingOtherVillager()) {
                            villager.goalSelector.enableControlFlag(Goal.Flag.MOVE);
                        }
                    }
                }
            }
        }
    }

    public NPCVillagerEntity getEntityByUUID (String EntityStringUUID) {
        for (VillagerData data : this.villagersData.values()) {
            NPCVillagerEntity villager = data.getEntity();
            if (villager != null) {
                if (villager.getStringUUID().equalsIgnoreCase(EntityStringUUID)) {
                    return villager;
                }
            }
        }
        return null;
    }

    public NPCVillagerEntity findTheNextVillagerToTalkTo (NPCVillagerEntity entity) {
        for (VillagerData data : this.villagersData.values()) {
            NPCVillagerEntity villager = data.getEntity();
            if (villager.getId() != entity.getId() && villager.getHasAwaken()) {
                // No awaken villager could not be chatted
                if (villager.getCustomVillagename().equals(entity.getCustomVillagename())){
                    if (!villager.getNavigation().isInProgress() && !villager.getControlWalkForceTrigger() && !villager.getIsTalkingWithOtherVillager() && !villager.getControlWalkIsWaitingOtherVillager()) {
                        LOGGER.warn(String.format("Villager %s found %s to talk to.",
                                entity.getName().getString(), villager.getName().getString()));
                        return villager;
                    }
                }
            }
        }
        return null;
    }

    public ArrayList<NPCVillagerEntity> getNeareFacedVillager(PlayerEntity player) {
        Vector3d playerPos = player.position();
        Vector3d playerLook = player.getViewVector(0.5f);

        ArrayList<NPCVillagerEntity> nearCustomVillagerList = new ArrayList<>();

        for (VillagerData data : this.villagersData.values()) {
            NPCVillagerEntity villager = data.getEntity();
            if (villager != null) {
                Vector3d villagerPos = villager.position();
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

    public String getVillageNameIronGolemAt (LivingEntity entity) {
        if (entity instanceof IronGolemEntity) {
            BlockPos b0 = entity.blockPosition();
            for (String name:this.villagesList.keySet()){
                BlockPos villagePos = this.villagesList.get(name);
                double distance = villagePos.distSqr(b0);
                if (distance < 100) {
                    return name;
                }
            }
        }
        return "Default";
    }

    public void tellAllVillagersTheirGolemHasBeenKilledByPlayer(IronGolemEntity entity,
                                                                @Nullable PlayerEntity player){
        String villageName = getVillageNameIronGolemAt(entity);
        List<NPCVillagerEntity> villagersListToBeNotified = findVillagersAtSameVillage(villageName);
        if (villagersListToBeNotified.size() <= 0) {
            return;
        }

        StringBuilder description = new StringBuilder();
        if (player != null) {
            description.append("Iron Golem in the village is killed by Player ")
                    .append(player.getName().getString())
                    .append(" and no longer protecting the village.\n");
        } else {
            description.append("Iron Golem in the village is dead and no longer protecting the " +
                    "village.\n");
        }
        for (NPCVillagerEntity villager:villagersListToBeNotified) {
            villager.getBrain().setMemory(NPCVillagerMod.GOLEM_PROTECTING_MEMORY, description.toString());
        }
    }

    //region GETTER/SETTER
    public String getSsoToken() {
        return ssoToken;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }

    public String getAccessToken(UUID PlayerUUD) {
        return accessToken.get(PlayerUUD);
    }

    @Deprecated
    public String getAccessToken() {
        return null;
    }

    public void setAccessToken(UUID PlayerUUID, String accessToken) {
        this.accessToken.put(PlayerUUID, accessToken);
    }

    public String getAccessKey(UUID PlayerUUD) {
        return accessKey.get(PlayerUUD);
    }

    @Deprecated
    public String getAccessKey() {
        return null;
    }

    public void setAccessKey(UUID PlayerUUID, String accessKey) {
        this.accessKey.put(PlayerUUID, accessKey);
    }

    public void setOpenAIAPIKey(String apiKey) {
        this.openAIAPIKey = apiKey;
    }

    public String getOpenAIAPIKey() {
        return this.openAIAPIKey;
    }
    //endregion

    public Boolean isVerified(){
        if (this.ssoToken == null || this.accessKey_default == null || this.accessToken_default == null) {
            return false;
        }
        return true;
    }

    public Boolean isVerified(UUID PlayerUUID){
        if (this.ssoToken == null || this.accessKey.get(PlayerUUID) == null || this.accessToken.get(PlayerUUID) == null) {
            return false;
        }
        return true;
    }

    public String getAccessToken_default() {
        return accessToken_default;
    }

    public void setAccessToken_default(String accessToken_default) {
        this.accessToken_default = accessToken_default;
    }

    public String getAccessKey_default() {
        return accessKey_default;
    }

    public void setAccessKey_default(String accessKey_default) {
        this.accessKey_default = accessKey_default;
    }

    public static class VillagerData {
        private int id;
        private NPCVillagerEntity entity;

        public VillagerData(int id, NPCVillagerEntity entity) {
            this.id = id;
            this.entity = entity;
        }

        public int getId() {
            return id;
        }

        public NPCVillagerEntity getEntity() {
            return entity;
        }
    }
}
