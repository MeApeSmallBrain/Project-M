# Project:M
##### [OpenOSRS](http://opensosrs.com "OpenOSRS website") (A)utomation (P)rogramming (I)nterface.
The goal is to provide a layer of (familiar) comfort.
Here is a sample [script](https://github.com/DrManhatta/projectm-scripts "Project:M Scripts")!
```java
@Extension
@PluginDescriptor(
        name = "MChopper",
        enabledByDefault = false,
        description = "Cut all the wood",
        tags = {"OpenOSRS", "ProjectM", "Woodcutting", "Automation"},
        type = PluginType.SKILLING
)
public class MChopper extends MScript {

    @Inject
    private Client client;

    @Override
    public int loop() {
        if (MPlayer.isIdle()) {
            if (!MInventory.isFull()) {
                chopTrees();
            } else {
                dropTrees();
            }
        }
        return 1000;
    }



    private void chopTrees() {
        GameObject tree = new MObjects()
                .hasName("Tree", "Dead tree", "Oak", "Willow")
                .hasAction("Chop down")
                .isWithinDistance(MPlayer.location(), 20 )
                .result()
                .nearestTo(MPlayer.get());

        if (tree != null) {
            MInteract.GameObject(tree, "Chop down");
        }
    }

    private void dropTrees() {
        MInventory.dropAll("Logs", "Oak logs", "Willow logs");
    }
}
```