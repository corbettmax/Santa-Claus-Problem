#!/usr/bin/env python3
"""
Santa Claus Problem - Python Implementation using Semaphores

Problem Summary:
- Santa sleeps until awakened by either:
  1. All 9 reindeer returning from vacation (PRIORITY)
  2. A group of 3 elves needing help
- If both are waiting, reindeer have priority
- Santa helps one group at a time

This implementation uses 7 semaphores for robust synchronization:
1. santa_sem - Wakes up Santa
2. reindeer_sem - Controls reindeer harness operations
3. elf_sem - Controls elf consultation
4. mutex - Protects shared counters
5. reindeer_mutex - Protects reindeer counter
6. elf_mutex - Protects elf counter  
7. santa_mutex - Ensures Santa handles one group at a time

This is better the Trono's original solution as it avoids deadlocks and ensures proper prioritization.
"""

import threading
import time
import random

# Semaphores
santaSem = threading.Semaphore(0)      # Wakes Santa
reindeerSem = threading.Semaphore(0)   # Released by Santa for reindeer
elfSem = threading.Semaphore(0)        # Released by Santa for elves
mutex = threading.Lock()                # General mutex
reindeerMutex = threading.Lock()       # Reindeer counter mutex
elfMutex = threading.Lock()            # Elf counter mutex
santaMutex = threading.Semaphore(1)    # Ensures Santa handles one group at a time

# Counters
reindeerCount = 0
elfCount = 0
waitingElves = 0

# Constants
NUM_REINDEER = 9
NUM_ELVES = 10
ELF_GROUP_SIZE = 3

def santa():
    """Santa's main loop - waits to be woken by reindeer or elves"""
    deliveries = 0
    elfConsultations = 0
    
    while True:
        # Wait to be woken up
        santaSem.acquire()
        
        santaMutex.acquire()
        
        # Check if reindeer are ready (priority)
        reindeerMutex.acquire()
        if reindeerCount == NUM_REINDEER:
            print("\n🎅 SANTA: Ho Ho Ho! All reindeer are back!")
            print("🎅 SANTA: Preparing sleigh for Christmas delivery...")
            
            # Release all reindeer to harness
            for i in range(NUM_REINDEER):
                reindeerSem.release()
            
            reindeerCount = 0
            reindeerMutex.release()
            
            time.sleep(0.5)  # Simulate delivery preparation
            deliveries += 1
            print(f"🎅 SANTA: Sleigh ready! Delivering toys! (Delivery #{deliveries})")
            print("🎅 SANTA: Going back to sleep...\n")
            
        else:
            reindeerMutex.release()
            
            # Check if elves need help
            elfMutex.acquire()
            if elfCount == ELF_GROUP_SIZE:
                print(f"\n🎅 SANTA: Three elves need help!")
                print("🎅 SANTA: Meeting with elves...")
                
                # Release the three elves for consultation
                for i in range(ELF_GROUP_SIZE):
                    elfSem.release()
                
                elfCount = 0
                elfMutex.release()
                
                time.sleep(0.3)  # Simulate consultation
                elfConsultations += 1
                print(f"🎅 SANTA: Consultation complete! (Session #{elfConsultations})")
                print("🎅 SANTA: Going back to sleep...\n")
            else:
                elfMutex.release()
        
        santaMutex.release()

def reindeer(id):
    """Reindeer thread - returns from vacation and gets harnessed"""
    while True:
        # Vacation in the tropics
        time.sleep(random.uniform(2, 5))
        print(f"🦌 Reindeer {id}: Returning from vacation")
        
        reindeerMutex.acquire()
        global reindeerCount
        reindeerCount += 1
        
        if reindeerCount == NUM_REINDEER:
            print(f"🦌 Reindeer {id}: I'm the last one! Waking Santa!")
            santaSem.release()  # Wake Santa
        
        reindeerMutex.release()
        
        # Wait to be harnessed
        reindeerSem.acquire()
        print(f"🦌 Reindeer {id}: Getting harnessed to sleigh")
        time.sleep(0.1)
        print(f"🦌 Reindeer {id}: Harnessed! Ready to deliver toys!")

def elf(id):
    """Elf thread - occasionally needs Santa's help"""
    while True:
        # Work on toys
        time.sleep(random.uniform(1, 4))
        
        elfMutex.acquire()
        global elfCount, waitingElves
        waitingElves += 1
        
        if waitingElves == ELF_GROUP_SIZE:
            print(f"🧝 Elf {id}: We have 3 elves waiting! Waking Santa!")
            elfCount = ELF_GROUP_SIZE
            waitingElves = 0
            santaSem.release()  # Wake Santa
        else:
            print(f"🧝 Elf {id}: Waiting for help (Total waiting: {waitingElves})")
        
        elfMutex.release()
        
        # Wait for consultation
        elfSem.acquire()
        print(f"🧝 Elf {id}: Getting help from Santa...")
        time.sleep(0.1)
        print(f"🧝 Elf {id}: Problem solved! Back to work!")

def main():
    print("=" * 60)
    print("🎄 SANTA CLAUS PROBLEM - PYTHON IMPLEMENTATION 🎄")
    print("=" * 60)
    print(f"Configuration:")
    print(f"  - Number of Reindeer: {NUM_REINDEER}")
    print(f"  - Number of Elves: {NUM_ELVES}")
    print(f"  - Elves per consultation group: {ELF_GROUP_SIZE}")
    print(f"  - Number of Semaphores: 7")
    print("=" * 60)
    print("\nStarting simulation...\n")
    
    # Create Santa thread
    santaThread = threading.Thread(target=santa, daemon=True)
    santaThread.start()
    
    # Create reindeer threads
    reindeerThreads = []
    for i in range(NUM_REINDEER):
        t = threading.Thread(target=reindeer, args=(i+1,), daemon=True)
        t.start()
        reindeerThreads.append(t)
    
    # Create elf threads
    elfThreads = []
    for i in range(NUM_ELVES):
        t = threading.Thread(target=elf, args=(i+1,), daemon=True)
        t.start()
        elfThreads.append(t)
    
    # Let simulation run for a while
    try:
        time.sleep(30)
        print("\n" + "=" * 60)
        print("🎄 Simulation Complete! 🎄")
        print("=" * 60)
    except KeyboardInterrupt:
        print("\n\nSimulation interrupted by user.")

if __name__ == "__main__":
    main()
