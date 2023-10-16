# Event Bus

## Purpose
An Event Bus is a powerful and flexible design pattern used in software development to facilitate communication and decoupling between different components of an application. It acts as a message broker that allows different parts of your code to communicate without having direct references to each other. An event can happen in your program, say a ```UserInputEvent```, and other parts of your code can quickly respond to and know the details of the event without being directly connected to the portion of your program responsible for initially posting the event.

## Key Features
**Post:** Post an event to your registered subscribers. The event will fire to them in order of priority.\
**Register:** Register either a static class or virtual class instance to the event bus. Every static method or virtual method will be registered with it, respectively.\
**Unregister:** Unregister a static class or virtual class instance from the bus, and its methods will stop receiving events.

## Usage
**Setup:** Include the library in your project.\
**Creating the Event Bus:** Start by creating a new instance of the ```EventBus```. You can create as many instances as you'd like, but a singleton instance should do for most projects.
```java
EventBus bus = new EventBus();
```
Next, extend the ```Event``` class to create your own custom events.
```java
public class UserInputEvent extends Event
{
    // Example keyboard input event that records an integer key code
    public final int key;

    public UserInputEvent(int key)
    {
        this.key = key;
    }
}
```
Now, implement parts of your code that need to know when the event fires, for example when a user presses a key.
```java
@SubscribeEvent
public void onUserInput(UserInputEvent event)
{
    log("User pressed key %s", event.key)
}
```
Make sure to post the event when the event happens.
```java
public void onKeyPress(int keyCode)
{
    UserInputEvent event = new UserInputEvent(keyCode);
    bus.post(event);
}
```
If you want one method to fire before another, use the priority value in ```SubscribeEvent```. Higher values fire first, the default is 0.
```java
@SubscribeEvent
public void fireSecond(Event event)
{
    // fires second
}

@SubscribeEvent(priority = 999)
public void fireFirst(Event event)
{
    // fires first, for the same event
}
```
## Benchmarks
This project uses a linked list data structure in order to hold subscribers in decreasing order of their priority. This allows us to quickly manage subscribers and their priorities without the large space complexity held by some other event buses that require large hashmaps and lists to keep track of events and subscribers. It also eliminates the need to calculate priority at runtime, since subscribers hold their positions in the list based on priority, where the first is the most important and the last is the least important. This allows us to traverse down the list to fire events without any rearranging or searching among the subscribers.\
\
The event bus can quickly dispatch 10,000 events to 1,000 subscribers in under a tenth of a second, and could easily handle more. The benchmark test can be found in ```src/test/```.\
![benchmark](https://i.imgur.com/6rcNB7k.png)

