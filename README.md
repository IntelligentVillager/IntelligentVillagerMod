![](/src/main/resources/logo.png)

## IntelligentVillagerMod

### First...
- Download Mod at [Official Webpage](https://intelligentVillager.com) or at the find the   
  release tag
- Join [Discord](https://discord.gg/TfbWmzSSmq) for further communication and join the project
- Visit [Youtube](https://www.youtube.com/channel/UCs8BE7kj9HDwOjFZZmyv2Ug) for more interesting recordings

### What is Intelligent Villagers?
Imagine there is a Minecraft server, where all the villagers are having unique personalities,   
personal background, history, personal relationships or even narratives under customized world   
of view (apparently this will be controlled only by the server owner :D), and all of them could   
have real-time perception and memory about the environment context, what experience can you   
explore in this world?

**Welcome to the West World and let's build it with the almighty MINECRAFT!**

### How does an Intelligent Villager work?

All intelligent villagers are powered by **OpenAI's GPT Large Language Models** and **rct AI's   
virtual character creation platform, Socrates**. Our villagers support:

1. Randomly generating village names and generating corresponding histories for villages   
   through the background generation of villagers. Each villager will know information about the   
   village he/she lives in.

2. Generating character and background profiles of each villager through GPT3, and feed them to   
   Socrates as the initial settings to generate the corresponding `Soul`. During the generation   
   process, the relationship between characters is also generated, and we can also explicitly   
   configure their knowledge graph on Socrates for each villager, which contains the relationship   
   between all characters.

3. The `Soul` generated on the Socrates platform enable the characters to interact based on a   
   specific   
   worldview with not only the players but also all the other Intelligent Villagers.

4. Thus, villagers will randomly seek out other villagers they can chat with through the   
   `randomChatGoal` goal, find them and start a conversation. At this point, players walking   
   nearby will be able to hear what they are chatting about. We use GPT3 to generate an initial   
   conversation for them around a specific topic (like weather, strange things they saw recently,  
   etc.), which leads to more interesting interactions between villagers.

5. Of course, besides listening to what the villagers are talking about every day, it is the   
   greatest fun for players to explore and discover different Souls and make friends with them.   
   Players can talk with the villagers through natural language, and can do some simple   
   interaction, such as `punch`, `pat`, and can even give the villagers some items, the villagers   
   will also generate behaviors to feedback. In addition to behavior, each villager will   
   also have an extra rendered layer on top of their head that shows the villager's current mood through expressions.

6. Another important module is `context management`, which works very well with the AI system   
   that comes with minecraft. Simply put, the `Sensor` gets a sense of the environment and manages all the `Memory` through the `Brain`, while for the Intelligent Villager, we just need to build and update the `Context` through the `Memory` according to a certain tick, so that the villagers can give more intelligent feedback in real time. For example:

* When you are looking for a villager, you can ask around and the villager who has seen the villager you are looking for is likely to tell you where and when he has seen him.

* When a player kills the Iron Golem in the village, in the long-term memory of all the   
  villagers, they will always remember this player, if this player speaks to them or visit the   
  village, he will have to suffer.

* Others including villagers' information, weather and time, and other real-time perceptions are also assembled in context. The more context there is, the more intelligent the villager must be, for example, it can remember different players differently. When you have more friendly interaction with a villager, he will be more friendly to you.

### Why I open source the project?

**This will be a large and complex project.** Turning villagers into intelligent villagers is actually creating narrative and experiential content for Minecraft that can be procedurally generated, which will make Minecraft the first game in the world where, in addition to the game world being procedurally generated, the interactive content players can experience in the game is also procedurally produced, which means infinite possibilities.

At the same time, Minecraft is so great mainly because of all the community developers who work for it, because of the millions of Mods and ResourcePacks, and because of the millions of Server Owners who are constantly creating new content and possibilities for Minecraft. Therefore, I hope that more developers and enthusiasts from the community who are interested in creating such a future for Minecraft will join our project and together we will make this open source project better and bring a new experience to players. The way to contribute will not be difficult, based on the current structure, although we have many many features and optimization to do, it's  also quite clear on the direction we are going to. So, if you are a Minecraft player, or a developer, or a Java engineer, or even without knowing anything about Java (which ChatGPT can actually help you learn really soon :D) that are interested in joining the project, welcome! You only need to fork the repository, setup locally, do anything you think is interesting or helpful, then submit a PR, boom! Your name will then be presented here and also on the webpage I created for this Mod.

### Requirements

- JDK8 or above (8 for best compatibility)
- IntelliJ IDEA / Eclipse
- 1.16.5 Java Edition + Forge 36.2.34 (on both Server and Client sides)

### Setup Process:

Step 1: Fork and clone the repository to your local.

Step 2: You're left with a choice.  
If you prefer to use Eclipse:
1. Run the following command: `gradlew genEclipseRuns` (`./gradlew genEclipseRuns` if you are on Mac/Linux)
2. Open Eclipse, Import > Existing Gradle Project > Select Folder   
   or run `gradlew eclipse` to generate the project.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: `gradlew genIntellijRuns` (`./gradlew genIntellijRuns` if you are on Mac/Linux)
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can   
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything   
{this does not affect your code} and then start the process again.

Step3: Once Gradle build is finished, select 'runServer' and 'runClient' in the Gradle task for   
debugging.

### Project Structure Breakdown

(I will come back on this part later :P)


### Additional Resources:
Forge Forum: https://forums.minecraftforge.net/
Forge Discord: https://discord.gg/UvedJ9m