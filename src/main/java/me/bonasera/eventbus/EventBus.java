package me.bonasera.eventbus;

import me.bonasera.eventbus.annotation.SubscribeEvent;
import me.bonasera.eventbus.event.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * This event bus uses a linked list structure to store events in a chain
 * in order of their priority, and then fires events linearly down the list
 * so there is no need for runtime priority calculation.
 *
 * @author Andrew Bonasera
 */
public final class EventBus
{
    /**
     * The first and highest priority invoker.
     */
    private Invoker head;

    public void register(Object subscriber)
    {
        if (subscriber instanceof Class<?>)
        {
            registerStaticSubscriber((Class<?>) subscriber);
        } else
        {
            registerVirtualSubscriber(subscriber);
        }
    }

    /**
     * Remove a subscriber from the linked list.
     */
    public void unregister(Object subscriber)
    {
        if (this.head == null)
        {
            return;
        }

        while (this.head != null && this.head.instance == subscriber)
        {
            this.head = this.head.next;
        }

        Invoker current = this.head;
        if (current == null)
        {
            return;
        }

        while (current.next != null)
        {
            if (current.next.instance == subscriber)
            {
                current.next = current.next.next;
            } else
            {
                current = current.next;
            }
        }
    }

    public void post(Event event)
    {
        Class<? extends Event> eventType = event.getClass();

        Invoker current = this.head;
        while (current != null)
        {
            if (eventType.equals(current.eventType))
            {
                this.tryInvokeMethod(current.method, current.instance, event);
            }

            current = current.next;
        }
    }

    /**
     * Registers all static methods on the given class.
     */
    private void registerStaticSubscriber(Class<?> subscriber)
    {
        Arrays.stream(subscriber.getDeclaredMethods())
                .filter(method ->
                        method.isAnnotationPresent(SubscribeEvent.class)
                                && method.getParameters().length == 1
                                && Event.class.isAssignableFrom(method.getParameters()[0].getType())
                                && Modifier.isStatic(method.getModifiers())
                ).forEach(method ->
                {
                    int priority = method.getDeclaredAnnotation(SubscribeEvent.class).priority();
                    Invoker invoker = new Invoker(method, subscriber, priority);

                    insertInvoker(invoker);
                });
    }

    /**
     * Registers all virtual methods on the given instance.
     */
    private void registerVirtualSubscriber(Object subscriber)
    {
        Arrays.stream(subscriber.getClass().getDeclaredMethods())
                .filter(method ->
                        method.isAnnotationPresent(SubscribeEvent.class)
                                && method.getParameters().length == 1
                                && Event.class.isAssignableFrom(method.getParameters()[0].getType())
                                && !Modifier.isStatic(method.getModifiers())
                ).forEach(method ->
                {
                    int priority = method.getDeclaredAnnotation(SubscribeEvent.class).priority();
                    Invoker invoker = new Invoker(method, subscriber, priority);

                    insertInvoker(invoker);
                });
    }

    /**
     * Insert an invoker into the linked list, ordered by priority.
     */
    private void insertInvoker(Invoker invoker)
    {
        if (this.head == null || invoker.priority > head.priority)
        {
            invoker.next = this.head;
            this.head = invoker;
        } else
        {
            Invoker current = head;

            while (current.next != null && invoker.priority <= current.next.priority)
            {
                current = current.next;
            }

            invoker.next = current.next;
            current.next = invoker;
        }
    }

    /**
     * Tries to invoke a given method on a given instance with a given event.
     * If the call fails, or the method throws an exception, throw a runtime exception.
     */
    private void tryInvokeMethod(Method method,
                                 Object instance,
                                 Event event)
    {
        try
        {
            method.setAccessible(true);
            method.invoke(instance, event);
        } catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * This class represents a subscriber in the event bus that holds a
     * priority and enough information to invoke it.
     */
    private static final class Invoker
    {
        private final Method method;
        private final Object instance;
        private final Class<?/* extends Event */> eventType;
        private final int priority;

        /**
         * The next (nullable) invoker in the linked list
         */
        private Invoker next;

        private Invoker(Method method,
                        Object instance,
                        int priority)
        {
            this.method = method;
            this.instance = instance;
            this.eventType = method.getParameters()[0].getType();
            this.priority = priority;

            this.next = null;
        }
    }
}
