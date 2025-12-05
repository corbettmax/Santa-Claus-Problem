/*
Santa Claus Problem - GoLang Implementation
Problem Summary:
 * - Santa sleeps until awakened by either:
 *   1. All 9 reindeer returning from vacation (PRIORITY)
 *   2. A group of 3 elves needing help
 * - If both are waiting, reindeer have priority
 * - Santa helps one group at a time

Seven Semaphores/Mutexes Used:
1. santaSem		– wakes up santa
2. reindeerSem	– controls reindeer
3. elfSem		– controls elves
4. counterMutex	– protects shared counters updated by GoRoutines
5. reindeerMutex	– protects reindeer waiting counter updates
6. elfMutex		– protects elf waiting counter updates
7. santaMutex	– protects santa's state
*/
package main

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

const (
	NUM_REINDEER   = 9
	NUM_ELVES      = 10
	ELF_GROUP_SIZE = 3
	SIMULATION_TIME = 30 * time.Second
)

// Semepahores and Mutexes
var santaSem chan struct{}
var reindeerSem chan struct{}
var elfSem chan struct{}
var reindeerMutex sync.Mutex
var elfMutex sync.Mutex

// Counters
var reindeerCount int
var elfCount int
var waitingElves int

// Statistics
var statsMutex sync.Mutex
var deliveries int
var elfConsultations int

// Function: randomSleepMs
// Description: Gets random sleep time in milliseconds
func randomSleepMs(minMs, maxMs int) time.Duration {
	ms := rand.Intn(maxMs-minMs+1) + minMs
	return time.Duration(ms) * time.Millisecond
}

// Function: santaThread
// Description: Santa's main loop
func santaThread(wg *sync.WaitGroup) {
	defer wg.Done()
	fmt.Println("SANTA: Starting my shift at the North Pole!")

	for {
		// Wait Semaphore
		<-santaSem

		// Check reindeer first (priority)
		reindeerMutex.Lock()
		if reindeerCount == NUM_REINDEER {
			fmt.Println("\nSANTA: Ho Ho Ho! All reindeer are back!")
			fmt.Println("SANTA: Preparing sleigh for Christmas delivery...")

			// Release all reindeer to harness
			for i := 0; i < NUM_REINDEER; i++ {
				reindeerSem <- struct{}{}
			}

			reindeerCount = 0
			reindeerMutex.Unlock()

			time.Sleep(500 * time.Millisecond)
			statsMutex.Lock()
			deliveries++
			statsMutex.Unlock()

			fmt.Printf("SANTA: Sleigh ready! Delivering toys! (Delivery #%d)\n", deliveries)
			fmt.Println("SANTA: Going back to sleep...\n")

			continue
		}
		reindeerMutex.Unlock()

		// Check elves
		elfMutex.Lock()
		if elfCount == ELF_GROUP_SIZE {
			fmt.Println("\nSANTA: Three elves need help!")
			fmt.Println("SANTA: Meeting with elves...")

			// Release the three elves for consultation
			for i := 0; i < ELF_GROUP_SIZE; i++ {
				elfSem <- struct{}{}
			}

			elfCount = 0
			elfMutex.Unlock()

			time.Sleep(300 * time.Millisecond)
			statsMutex.Lock()
			elfConsultations++
			statsMutex.Unlock()

			fmt.Printf("SANTA: Consultation complete! (Session #%d)\n", elfConsultations)
			fmt.Println("SANTA: Going back to sleep...\n")
		} else {
			elfMutex.Unlock()
		}
	}
}

// Function: reindeerThread
// Description: Reindeer's main loop
func reindeerThread(id int, wg *sync.WaitGroup) {
	defer wg.Done()
	for {
		// Vacation in the tropics
		time.Sleep(randomSleepMs(2000, 5000))

		fmt.Printf("Reindeer %d: Returning from vacation\n", id)

		reindeerMutex.Lock()
		reindeerCount++
		if reindeerCount == NUM_REINDEER {
			fmt.Printf("Reindeer %d: I'm the last one! Waking Santa!\n", id)
			// Wake Santa (non-blocking send to avoid blocking if already signalled)
			select {
			case santaSem <- struct{}{}:
			default:
			}
		}
		reindeerMutex.Unlock()

		// Wait to be harnessed
		<-reindeerSem
		fmt.Printf("Reindeer %d: Getting harnessed to sleigh\n", id)
		time.Sleep(100 * time.Millisecond)
		fmt.Printf("Reindeer %d: Harnessed! Ready to deliver toys!\n", id)
	}
}

// Function: elfThread
// Description: Elf's main loop
func elfThread(id int, wg *sync.WaitGroup) {
	defer wg.Done()
	for {
		// Work on toys
		time.Sleep(randomSleepMs(1000, 4000))

		elfMutex.Lock()
		waitingElves++
		// If 3 elves are waiting, wake Santa
		if waitingElves == ELF_GROUP_SIZE {
			fmt.Printf(" Elf %d: We have 3 elves waiting! Waking Santa!\n", id)
			elfCount = ELF_GROUP_SIZE
			waitingElves = 0
			// Wake Santa (non-blocking)
			select {
				case santaSem <- struct{}{}:
				default:
			}
			// Else just print waiting status
		} else {
			fmt.Printf("Elf %d: Waiting for help (Total waiting: %d)\n", id, waitingElves)
		}
		elfMutex.Unlock()

		// Wait Semaphore elf help
		<-elfSem
		fmt.Printf("Elf %d: Getting help from Santa...\n", id)
		time.Sleep(100 * time.Millisecond)
		fmt.Printf("Elf %d: Problem solved! Back to work!\n", id)
	}
}

func main() {
	rand.Seed(time.Now().UnixNano())

	fmt.Println("============================================================")
	fmt.Println(" SANTA CLAUS PROBLEM - Go Implementation ")
	fmt.Println("============================================================")
	fmt.Printf("Configuration:\n")
	fmt.Printf("  - Number of Reindeer: %d\n", NUM_REINDEER)
	fmt.Printf("  - Number of Elves: %d\n", NUM_ELVES)
	fmt.Printf("  - Elves per consultation group: %d\n", ELF_GROUP_SIZE)
	fmt.Printf("  - Semaphores implemented via channels\n")
	fmt.Println("============================================================")
	fmt.Println("\nStarting simulation...\n")

	// Initialize sempahores
	santaSem = make(chan struct{}, 1) // santaSem buffered 1 so multiple wake attempts don't block
	reindeerSem = make(chan struct{}, NUM_REINDEER) // reindeerSem unbuffered queues but used with multiple sends by Santa, so buffered to avoid being blocked
	elfSem = make(chan struct{}, ELF_GROUP_SIZE) // elfSem unbuffered queues but used with multiple sends by Santa, so buffered to avoid being blocked

	var wg sync.WaitGroup

	// Start Santa GoRoutine
	wg.Add(1)
	go santaThread(&wg)

	// Start Reindeer GoRoutines
	for i := 1; i <= NUM_REINDEER; i++ {
		wg.Add(1)
		go reindeerThread(i, &wg)
	}

	// Start Elf GoRoutines
	for i := 1; i <= NUM_ELVES; i++ {
		wg.Add(1)
		go elfThread(i, &wg)
	}

	// Let simulation run for SIMULATION_TIME, then exit
	time.Sleep(SIMULATION_TIME)

	// Print statistics (note: GoRoutines are not explicitly stopped; process exits)
	fmt.Println("\n============================================================")
	fmt.Println(" Simulation Complete! ")
	fmt.Println("============================================================")
	statsMutex.Lock()
	fmt.Printf("Statistics:\n")
	fmt.Printf("  - Total Deliveries: %d\n", deliveries)
	fmt.Printf("  - Total Elf Consultations: %d\n", elfConsultations)
	statsMutex.Unlock()
	fmt.Println("============================================================")
}
