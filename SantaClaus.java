/**
 * Santa Claus Problem - Java Implementation using Monitors
 *
 * Problem Summary:
 * - Santa sleeps until awakened by either:
 *   1. All 9 reindeer returning from vacation (PRIORITY)
 *   2. A group of 3 elves needing help
 * - If both are waiting, reindeer have priority
 * - Santa helps one group at a time
 *
 * This implementation uses Java monitors (synchronized/wait/notify) for synchronization.
 * Monitors provide mutual exclusion and condition synchronization through built-in Java features.
 * 
 * Key synchronization objects:
 * 1. santaLock - Controls Santa's wake/sleep cycle
 * 2. reindeerLock - Protects reindeer counter and coordinates harness operations
 * 3. elfLock - Protects elf counters and coordinates consultations
 */

import java.util.Random;

public class SantaClaus {
    // Constants
    private static final int NUM_REINDEER = 9;
    private static final int NUM_ELVES = 10;
    private static final int ELF_GROUP_SIZE = 3;
    private static final int SIMULATION_TIME = 30000; // milliseconds

    // Monitor locks
    private static final Object santaLock = new Object();
    private static final Object reindeerLock = new Object();
    private static final Object elfLock = new Object();

    // Counters
    private static int reindeerCount = 0;
    private static int elfCount = 0;
    private static int waitingElves = 0;
    
    // Flags to indicate which group is ready
    private static boolean reindeerReady = false;
    private static boolean elvesReady = false;
    
    // Flags to control when workers can proceed
    private static boolean reindeerCanHarness = false;
    private static boolean elvesCanConsult = false;
    private static int harnessedCount = 0;
    private static int consultedCount = 0;

    // Statistics
    private static int deliveries = 0;
    private static int elfConsultations = 0;

    // Random number generator
    private static Random random = new Random();

    /**
     * Santa thread - waits to be woken by reindeer or elves using monitors
     */
    static class Santa extends Thread {
        public void run() {
            System.out.println("SANTA: Starting shift at the North Pole");

            while (!Thread.interrupted()) {
                try {
                    synchronized (santaLock) {
                        // Wait until either reindeer or elves are ready
                        while (!reindeerReady && !elvesReady) {
                            santaLock.wait();
                        }
                    }

                    // Check if reindeer are ready (priority) - check outside santaLock
                    synchronized (reindeerLock) {
                        if (reindeerReady) {
                            handleReindeer();
                            continue; // Go back to waiting
                        }
                    }

                    // If no reindeer, check elves
                    synchronized (elfLock) {
                        if (elvesReady) {
                            handleElves();
                        }
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void handleReindeer() throws InterruptedException {
            System.out.println("\nSANTA: Ho Ho Ho! All reindeer are back!");
            System.out.println("SANTA: Preparing sleigh for Christmas delivery...");

            // Reset flags and counters
            reindeerReady = false;
            reindeerCount = 0;
            harnessedCount = 0;

            // Signal all reindeer to proceed with harnessing
            reindeerCanHarness = true;
            reindeerLock.notifyAll();

            // Wait for all reindeer to finish harnessing
            while (harnessedCount < NUM_REINDEER) {
                reindeerLock.wait();
            }

            reindeerCanHarness = false;
            
            // Wake up any reindeer that returned late and are waiting to start counting again
            reindeerLock.notifyAll();

            Thread.sleep(500); // Simulate delivery preparation
            deliveries++;
            System.out.println("SANTA: Sleigh ready! Delivering toys! (Delivery #" + deliveries + ")");
            System.out.println("SANTA: Going back to sleep...\n");
        }

        private void handleElves() throws InterruptedException {
            System.out.println("\nSANTA: Three elves need help!");
            System.out.println("SANTA: Meeting with elves...");

            // Reset flags and counters - but keep elfCount until after signaling
            elvesReady = false;
            consultedCount = 0;

            // Signal the three elves to proceed with consultation
            elvesCanConsult = true;
            elfLock.notifyAll();

            // Wait for all three elves to finish consultation
            while (consultedCount < ELF_GROUP_SIZE) {
                elfLock.wait();
            }

            // Now reset elfCount after all elves are done
            elfCount = 0;
            elvesCanConsult = false;

            Thread.sleep(300); // Simulate consultation
            elfConsultations++;
            System.out.println("SANTA: Consultation complete! (Session #" + elfConsultations + ")");
            System.out.println("SANTA: Going back to sleep...\n");
        }
    }

    /**
     * Reindeer thread - returns from vacation and gets harnessed using monitors
     */
    static class Reindeer extends Thread {
        private int id;

        public Reindeer(int id) {
            this.id = id;
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    // Vacation in the tropics
                    Thread.sleep(2000 + random.nextInt(3000));
                    System.out.println("Reindeer " + id + ": Returning from vacation");

                    synchronized (reindeerLock) {
                        // Wait until there's no active delivery and we can join the counting
                        while (reindeerCanHarness || reindeerCount >= NUM_REINDEER) {
                            reindeerLock.wait();
                        }

                        reindeerCount++;
                        boolean isPartOfGroup = (reindeerCount <= NUM_REINDEER);

                        if (reindeerCount == NUM_REINDEER) {
                            System.out.println("Reindeer " + id + ": I'm the last one! Waking Santa!");
                            
                            // Wake Santa
                            synchronized (santaLock) {
                                reindeerReady = true;
                                santaLock.notify();
                            }
                        }

                        // Only the first 9 reindeer wait for Santa
                        if (isPartOfGroup) {
                            // Wait for Santa to signal harnessing can begin
                            while (!reindeerCanHarness) {
                                reindeerLock.wait();
                            }

                            // Harness operation
                            System.out.println("Reindeer " + id + ": Getting harnessed to sleigh");
                            Thread.sleep(100);
                            System.out.println("Reindeer " + id + ": Harnessed! Ready to deliver toys!");

                            harnessedCount++;
                            if (harnessedCount == NUM_REINDEER) {
                                reindeerLock.notify(); // Wake Santa
                            }
                        }
                        // If not part of group, just continue and go back on vacation
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /**
     * Elf thread - occasionally needs Santa's help using monitors
     */
    static class Elf extends Thread {
        private int id;

        public Elf(int id) {
            this.id = id;
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    // Work on toys
                    Thread.sleep(1000 + random.nextInt(3000));

                    boolean isInGroup = false;

                    synchronized (elfLock) {
                        waitingElves++;

                        if (waitingElves == ELF_GROUP_SIZE) {
                            System.out.println("Elf " + id + ": We have 3 elves waiting! Waking Santa!");
                            elfCount = ELF_GROUP_SIZE;
                            waitingElves = 0;

                            // Wake Santa
                            synchronized (santaLock) {
                                elvesReady = true;
                                santaLock.notify();
                            }
                        } else {
                            System.out.println("Elf " + id + ": Waiting for help (Total waiting: " + waitingElves + ")");
                        }

                        // Wait for Santa to signal consultation can begin
                        while (elfCount > 0 && !elvesCanConsult) {
                            elfLock.wait();
                        }

                        // Check if this elf is part of the group being serviced
                        if (elvesCanConsult && consultedCount < ELF_GROUP_SIZE) {
                            isInGroup = true;
                            consultedCount++; // Reserve spot
                        }
                    }

                    // Perform consultation outside the lock if part of group
                    if (isInGroup) {
                        System.out.println("Elf " + id + ": Getting help from Santa...");
                        Thread.sleep(100);
                        System.out.println("Elf " + id + ": Problem solved! Back to work!");

                        synchronized (elfLock) {
                            if (consultedCount == ELF_GROUP_SIZE) {
                                elfLock.notify(); // Wake Santa
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("SANTA CLAUS PROBLEM - JAVA IMPLEMENTATION");
        System.out.println("============================================================");
        System.out.println("Configuration:");
        System.out.println("  - Number of Reindeer: " + NUM_REINDEER);
        System.out.println("  - Number of Elves: " + NUM_ELVES);
        System.out.println("  - Elves per consultation group: " + ELF_GROUP_SIZE);
        System.out.println("  - Synchronization: Java Monitors (synchronized/wait/notify)");
        System.out.println("============================================================");
        System.out.println("\nStarting simulation...\n");

        // Create Santa thread
        Santa santa = new Santa();
        santa.setDaemon(true);
        santa.start();

        // Create reindeer threads
        Reindeer[] reindeerThreads = new Reindeer[NUM_REINDEER];
        for (int i = 0; i < NUM_REINDEER; i++) {
            reindeerThreads[i] = new Reindeer(i + 1);
            reindeerThreads[i].setDaemon(true);
            reindeerThreads[i].start();
        }

        // Create elf threads
        Elf[] elfThreads = new Elf[NUM_ELVES];
        for (int i = 0; i < NUM_ELVES; i++) {
            elfThreads[i] = new Elf(i + 1);
            elfThreads[i].setDaemon(true);
            elfThreads[i].start();
        }

        // Let simulation run
        try {
            Thread.sleep(SIMULATION_TIME);
        } catch (InterruptedException e) {
            System.out.println("\nSimulation interrupted by user.");
        }

        System.out.println("\n============================================================");
        System.out.println("Simulation Complete!");
        System.out.println("============================================================");
        System.out.println("Statistics:");
        System.out.println("  - Total Deliveries: " + deliveries);
        System.out.println("  - Total Elf Consultations: " + elfConsultations);
        System.out.println("============================================================");
    }
}
