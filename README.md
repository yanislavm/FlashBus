# FlashBus


## Overview

**FlashBus** is the fastest Event Bus currently available that is very easy to be used and also saved from memory leaks.<br>
![benchmark](https://github.com/flymanj/FlashBus/blob/develop/examples/screenshots/Screenshot_20160612-192825.png "Bench Mark")
<br>
The benchmark application is available in examples/BenchmarkApp folder so you can check it's source code and run it locally. 

## How to use it
The usage is very simple.

1) Add dependancy to your build.gradle file

```
compile 'com.flashbus:flashbus:1.0.0'
```


2) Create a class that implements EventBus.IEvent interface. 

```
public class ExampleEvent implements EventBus.IEvent {}
```

3) Define a field into the class where you want to have a subscription for that particular event

```
private EventBus.IEventHandler<ExampleEvent> mEventHandler;
```
4) Subscribe for the Event

```
EventBus.getDefault().register(ExampleEvent.class, EventBus.ThreadMode.MAIN, mEventHandler);
```
Notice that the second parameter is the Thread where you want to receive the Event. The great advantage of FlashBus is that it can deliver the events directly to the main thread.