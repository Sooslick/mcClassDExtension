# Class D: Manhunt. Extra game modes

Custom game modes for [Class D: Manhunt](https://github.com/Sooslick/mcClassD).
In this Minecraft mini-game one player becomes the Victim and must complete an extraordinary task, 
while the rest become Hunters and have to kill the Victim.

We created this plugin to test the ClassD's API, 
and most probably we will create some extra game modes in the future - Manhunt-based DeathRun as an example.

### New game modes

* Block defense:

The Victim has to destroy a certain amount of special blocks that randomly spawn
in the world. The Victim also has the compass with needle pointing to the nearest special block whereas
Hunters' compasses are still tracking the Victim.

### Setup

1. Put the downloaded _ClassD_ and _ClassDExtension_ .jar files in your server's `/plugins` folder.  
2. Restart your server. _We do not recommend using 
`/reload` command because it may cause issues in plugins' work_  
3. Configure the generated config.yml in `/plugins/ClassDExtension`.  
4. Add custom game modes to primary ClassD config:  
4.1. Open the config.yml in `/plugins/ClassD`  
4.2. Copy lines to `gamemodes` list:  
`  defense: ru.sooslick.outlawExtension.gamemode.dbd.DbdExtensionBase`  
4.3. Change `preferredGamemode` to selected game mode and save the file
5. Restart your server again.
