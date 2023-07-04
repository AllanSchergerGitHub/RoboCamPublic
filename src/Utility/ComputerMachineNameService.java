package Utility;

/**
 * This is a singleton class - there is only one instance of this object and
 * I can call the same object from various other classes.
 * 
 * Service class to manage the machine name of the computer where the code is running.
 * 
 * This ComputerMachineNameService concept was proposed by chatGpt as a way to pass
 * values up and down the hierarchy without passing values from one 'layer' of the
 * object oriented hierarchy to the next. I sometimes am passing values 3 or more
 * layers and the values aren't used in the intermediary layers.
 * 
 * @author allan
 * code added June 2023
 * 
 * with the service/manager design idea - what are the downsides of this approach 
 * considering I have multiple objects spawning multiple objects that spawn even 
 * more objects? the intermidary objects don't need the values I'm passing in/out 
 * of the service/manager.
 * 
 * ChatGPT
 * The service/manager design pattern is a powerful tool for organizing and managing 
 * code, but like any design pattern, it has its pros and cons. Here are some potential 
 * downsides when considering a complex object hierarchy like yours:
 * 
 * Increased Complexity: Adding a service or manager class introduces another layer 
 * of abstraction to your code. This can make the code more difficult to understand 
 * and maintain, especially for developers who are not familiar with the service/manager pattern.
 * 
 * Tight Coupling: If many objects are interacting with the service/manager, it can lead to tight coupling. 
 * Changes to the service/manager class could potentially impact many parts of your codebase, 
 * making it harder to change and evolve your code over time.
 * 
 * Indirection: The service/manager class acts as an intermediary between objects. 
 * This can make it harder to trace the flow of data and control through your code, 
 * especially when debugging.
 * 
 * State Management: If the service/manager class needs to maintain state, 
 * managing and synchronizing that state can become complex, especially in a 
 * multithreaded environment.
 * 
 * Testing Challenges: Testing code that relies heavily on services or 
 * managers can be challenging. You may need to use techniques like mocking or 
 * stubbing to isolate the code under test, which can add to the complexity 
 * of your test suite.
 * 
 * In your specific case, if intermediary objects don't need the values you're 
 * passing in/out of the service/manager, it might be worth reconsidering whether 
 * you need a service/manager at all. It might be that a simpler design pattern, 
 * or a refactoring of your object hierarchy, could meet your needs with less complexity.
 * 
 * could there be simultaneous writes to the service that would collide?
 * 
 * ChatGPT
 * Yes, in a multi-threaded environment, simultaneous writes to a shared resource 
 * like a service can potentially lead to race conditions, where the outcome depends 
 * on the relative timing of the threads' execution. This can lead to inconsistent 
 * or unexpected results.
 * 
 * For example, consider a service that maintains a counter. If two threads simultaneously 
 * read the counter's value, increment it, and write the new value back to the service, 
 * the counter might only be incremented once instead of twice. This is because both 
 * threads read the original value of the counter before either of them had a chance 
 * to write the incremented value back to the service.
 * 
 * To prevent race conditions, you can use synchronization mechanisms like locks or 
 * semaphores to ensure that only one thread can write to the service at a time. In Java, 
 * you can use the synchronized keyword to create a critical section, where only one 
 * thread can execute the code within the critical section at a time.
 * 
 * However, synchronization can also lead to other issues like deadlocks, where two 
 * or more threads are each waiting for the other to release a lock, and none of them 
 * can proceed. Therefore, it's important to use synchronization carefully and to 
 * design your code in a way that minimizes the potential for these kinds of concurrency issues.
 * 
 */
public class ComputerMachineNameService {
    private static ComputerMachineNameService instance = null;
    private String computerMachineName;
    private String providerConsumerTag;

    /**
     * Constructs a new ComputerMachineNameService.
     * This is private so it can't be called directly so we can maintain this as a singleton.
     * @param initialComputerMachineName either Rover or UI; to identify which computer
     * this code is running on.
     * @param providerConsumerTag to help with debugging, this identifies which objects,
     * etc are providing or consuming the data of this service.
     */
    private ComputerMachineNameService(String initialComputerMachineName, String providerConsumerTag) {
        this.computerMachineName = initialComputerMachineName;
        this.providerConsumerTag = providerConsumerTag;
    }
        
    /**
     * Use this to return an instance.
     * On the first call to this method it will create an instance if it doesn't already exist.
     * @param providerConsumerTag is a "who's asking?" type of field for tracking where requests are coming from.
     * @return
     */
    public static ComputerMachineInfo getComputerMachineNameInstance(String providerConsumerTag) {
        if (instance == null) {
            return null;
        } else {
            return instance.getPrivateComputerMachineName(providerConsumerTag);
        }
    }
    
    private ComputerMachineInfo getPrivateComputerMachineName(String providerConsumerTag) {
        this.providerConsumerTag = providerConsumerTag;
        return new ComputerMachineInfo(this.computerMachineName, this.providerConsumerTag);
    }

    /**
     * On the first call to this method it will create the instance.
     * This might not be the best practice as it should probably be created
     * via a specific single purpose method designed for that purpose.
     * Sets the machine name of the computer. Either Rover or UI; to identify which computer
     * this code is running on.
     *
     * @param newComputerMachineName The new machine name.
     * @param providerConsumerTag A tag to identify which objects are providing or consuming the data of this service.
     */
    public static void setComputerMachineName(String newComputerMachineName, String providerConsumerTag) {
        if (instance == null) {
            instance = new ComputerMachineNameService(newComputerMachineName, providerConsumerTag);
        } else {
            instance.computerMachineName = newComputerMachineName;
            instance.providerConsumerTag = providerConsumerTag;
        }
    }
    
    /**
     * Inner class
     */
    public static class ComputerMachineInfo {
        private final String computerMachineName;
        private final String providerConsumerTag;

        public ComputerMachineInfo(String computerMachineName, String providerConsumerTag) {
            this.computerMachineName = computerMachineName;
            this.providerConsumerTag = providerConsumerTag;
        }
        
        public String getComputerMachineName() {
            return this.computerMachineName;
        }

        public String getProviderConsumerTag() {
            return this.providerConsumerTag;
        }
    }
}
