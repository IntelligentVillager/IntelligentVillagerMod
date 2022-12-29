package com.sergio.ivillager;


import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ClientChatInject {
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
            // change event
            String modifiedMessage = String.format("<villager command> %s", message);
            event.setMessage(modifiedMessage);
            event.setCanceled(false);
        }

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
