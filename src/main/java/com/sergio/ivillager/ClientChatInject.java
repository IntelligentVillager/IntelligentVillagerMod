package com.sergio.ivillager;


import com.sergio.ivillager.NPCVillager.CustomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class ClientChatInject {

    public static final Logger LOGGER = LogManager.getLogger(ClientChatInject.class);

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onChatMessage(ClientChatEvent event) {
        // Get the message that was sent
        String message = event.getMessage();

        // Do something with the message, such as logging it or performing some action based on its content
        System.out.println("Received chat message: " + message);

        // TODO: 检查是否在和 NPC 聊天，把部分position 判断逻辑放这里来，也可以增加更多的session 判断，比如是否已经处于聊天状态
        // TODO: 把 onServerChat 的里部分 position 的信息直接在这里判断，包括 NPC 的位置（甚至是 NPC 的ID），然后打包到message 里直接发给服务端
        // TODO: 服务端就可以减少部分操作了，毕竟这也需要计算的

        if (message.startsWith("/")) {
        } else {
            List<Entity> nearbyEntities = getNearbyEntities(getCurrentPlayer(),
                    getCurrentPlayer().getEntity().level, 5.0);

            // change event
            String modifiedMessage = String.format("<villager command> %s", message);
            event.setMessage(modifiedMessage);
            event.setCanceled(false);
        }

    }

    @OnlyIn(Dist.CLIENT)
    public ArrayList<CustomEntity> getNeareFacedVillager(PlayerEntity player, List<Entity> nearByEntities) {
        Vector3d playerPos = player.position();
        Vector3d playerLook = player.getViewVector(0.5f);

        ArrayList<CustomEntity> nearCustomVillagerList = new ArrayList<>();

        for (Entity data : nearByEntities) {
            if (data instanceof CustomEntity) {
                Vector3d villagerPos = data.position();
                Vector3d playerToVillager = villagerPos.subtract(playerPos);
                double distance = playerToVillager.length();
                if (distance < 6 && playerLook.dot(playerToVillager) > 0) {
                    nearCustomVillagerList.add((CustomEntity) data);
                }
            }
        }
        LOGGER.warn(String.format("%d villagers in client world instance, %d in interactive " +
                "range", nearByEntities.size(),nearCustomVillagerList.size()));

        return nearCustomVillagerList;
    }

    @OnlyIn(Dist.CLIENT)
    public static PlayerEntity getCurrentPlayer() {
        return Minecraft.getInstance().player;
    }

    @OnlyIn(Dist.CLIENT)
    public static List<Entity> getNearbyEntities(PlayerEntity player, World world, double radius) {
        double x0 = player.position().x;
        double y0 = player.position().y;
        double z0 = player.position().z;
        AxisAlignedBB boundingBox = new AxisAlignedBB(x0 - radius, y0 - radius, z0 - radius, x0 + radius, y0 + radius, z0 + radius);
        return world.getEntities(player, boundingBox);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onClientReceivedChat(ClientChatReceivedEvent event) throws Exception {
        System.out.println("FUCK!!!!");
        System.out.println("Received chat received event " + event.getMessage());
        // Get the message that was received
        ITextComponent message = event.getMessage();

        String rawMsg = message.getString();
        System.out.println("receive msg: " + message.getString());
        if (rawMsg.contains("<villager command>")) {

            // Modify the message
            ITextComponent modifiedMessage = new StringTextComponent(rawMsg.replace("<villager command> ", ""));

            // Set the modified message
            event.setMessage(modifiedMessage);
            event.setCanceled(false);
        } else if (rawMsg.contains("<villager response>")) {

            String msg = rawMsg.replace("<villager response>", "");
            String[] msgs = msg.split(">", 2);

            ITextComponent nameString = new StringTextComponent(String.format("%s>",
                    msgs[0]))
                    .setStyle(Style.EMPTY.withColor(TextFormatting.BLUE));
            ITextComponent contentString = new StringTextComponent(msgs[1])
                    .setStyle(Style.EMPTY);

            TextComponent messageComponent = new TextComponent() {
                @Override
                public TextComponent plainCopy() {
                    return null;
                }
            };

            messageComponent.append(nameString);
            messageComponent.append(contentString);

            event.setMessage(messageComponent);
            event.setCanceled(false);
        }

    }
}
