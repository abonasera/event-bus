package me.bonasera.eventbus;

import me.bonasera.eventbus.annotation.SubscribeEvent;
import me.bonasera.eventbus.event.Event;

public final class Main
{
    public static void main(String[] args)
    {
        // Create a new event bus
        EventBus bus = new EventBus();

        // Create a new test event
        TestEvent event = new TestEvent("Test Message");

        // Register static methods
        bus.register(Main.class);

        // Create and register instance
        Main main = new Main();
        bus.register(main);

        // Post event
        bus.post(event);

        // Unregister static
        bus.unregister(Main.class);

        // Unregister instance
        bus.unregister(main);

        // Benchmark event firing times
        for (int i = 0; i < 1000; i++)
        {
            bus.register(new DummySubscriber());
        }

        long startTime = System.nanoTime();

        for (int i = 0; i < 10000; i++)
        {
            bus.post(new TestEvent(""));
        }

        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1e9;

        System.out.printf(
                "\nFired 10,000 events to 1,000 subscribers in %s seconds.\n",
                elapsedTime
        );
    }

    @SubscribeEvent
    public static void staticMethod(TestEvent event)
    {
        System.out.printf(
                "\nStatic method received TestEvent with msg=%s\n",
                event.msg
        );
    }

    @SubscribeEvent(priority = 999)
    public void virtualMethod(TestEvent event)
    {
        System.out.printf(
                "\nVirtual method received TestEvent with msg=%s\n",
                event.msg
        );
    }

    private static class TestEvent extends Event
    {
        private final String msg;

        private TestEvent(String msg)
        {
            this.msg = msg;
        }
    }

    private static class DummySubscriber
    {
        @SubscribeEvent
        public void onTestEvent(TestEvent event)
        {
        }
    }
}
