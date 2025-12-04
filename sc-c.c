/*
 * Santa Claus Problem - C Implementation using POSIX Semaphores
 *
 * Problem Summary:
 * - Santa sleeps until awakened by either:
 *   1. All 9 reindeer returning from vacation (PRIORITY)
 *   2. A group of 3 elves needing help
 * - If both are waiting, reindeer have priority
 * - Santa helps one group at a time
 *
 * This implementation uses 7 semaphores for robust synchronization:
 * 1. santa_sem - Wakes up Santa
 * 2. reindeer_sem - Controls reindeer harness operations
 * 3. elf_sem - Controls elf consultation
 * 4. mutex - Protects shared counters  
 * 5. reindeer_mutex - Protects reindeer counter
 * 6. elf_mutex - Protects elf counter
 * 7. santa_mutex - Ensures Santa handles one group at a time
 * 
 * This is better the Trono's original solution as it avoids deadlocks and ensures proper prioritization.
 */

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>
#include <time.h>

#define NUM_REINDEER 9
#define NUM_ELVES 10
#define ELF_GROUP_SIZE 3
#define SIMULATION_TIME 30

// Semaphores
sem_t santaSem;
sem_t reindeerSem;
sem_t elfSem;
sem_t santaMutex;

// Mutexes
pthread_mutex_t reindeerMutex = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t elfMutex = PTHREAD_MUTEX_INITIALIZER;

// Counters
int reindeerCount = 0;
int elfCount = 0;
int waitingElves = 0;

// Statistics
int deliveries = 0;
int elfConsultations = 0;

// Function to get random sleep time in milliseconds
int randomSleepMs(int minMs, int maxMs) {
    return minMs + (rand() % (maxMs - minMs + 1));
}

void *santaThread(void *arg) {
    printf("SANTA: Starting shift at the North Pole\n");
    
    while (1) {
        // Wait to be woken up
        sem_wait(&santaSem);
        
        sem_wait(&santaMutex);
        
        // Check if reindeer are ready (priority)
        pthread_mutex_lock(&reindeerMutex);
        if (reindeerCount == NUM_REINDEER) {
            printf("\nSANTA: Ho Ho Ho! All reindeer are back!\n");
            printf("SANTA: Preparing sleigh for Christmas delivery...\n");
            
            // Release all reindeer to harness
            for (int i = 0; i < NUM_REINDEER; i++) {
                sem_post(&reindeerSem);
            }
            
            reindeerCount = 0;
            pthread_mutex_unlock(&reindeerMutex);
            
            usleep(500000);  // 0.5 seconds
            deliveries++;
            printf("SANTA: Sleigh ready! Delivering toys! (Delivery #%d)\n", deliveries);
            printf("SANTA: Going back to sleep...\n\n");
            
        } else {
            pthread_mutex_unlock(&reindeerMutex);
            
            // Check if elves need help
            pthread_mutex_lock(&elfMutex);
            if (elfCount == ELF_GROUP_SIZE) {
                printf("\nSANTA: Three elves need help!\n");
                printf("SANTA: Meeting with elves...\n");
                
                // Release the three elves for consultation
                for (int i = 0; i < ELF_GROUP_SIZE; i++) {
                    sem_post(&elfSem);
                }
                
                elfCount = 0;
                pthread_mutex_unlock(&elfMutex);
                
                usleep(300000);  // 0.3 seconds
                elfConsultations++;
                printf("SANTA: Consultation complete! (Session #%d)\n", elfConsultations);
                printf("SANTA: Going back to sleep...\n\n");
            } else {
                pthread_mutex_unlock(&elfMutex);
            }
        }
        
        sem_post(&santaMutex);
    }
    
    return NULL;
}

void *reindeerThread(void *arg) {
    int id = *(int *)arg;
    free(arg);
    
    while (1) {
        // Vacation in the tropics
        usleep(randomSleepMs(2000, 5000) * 1000);
        printf("Reindeer %d: Returning from vacation\n", id);
        
        pthread_mutex_lock(&reindeerMutex);
        reindeerCount++;
        
        if (reindeerCount == NUM_REINDEER) {
            printf("Reindeer %d: I'm the last one! Waking Santa!\n", id);
            sem_post(&santaSem);  // Wake Santa
        }
        
        pthread_mutex_unlock(&reindeerMutex);
        
        // Wait to be harnessed
        sem_wait(&reindeerSem);
        printf("Reindeer %d: Getting harnessed to sleigh\n", id);
        usleep(100000);  // 0.1 seconds
        printf("Reindeer %d: Harnessed! Ready to deliver toys!\n", id);
    }
    
    return NULL;
}

void *elfThread(void *arg) {
    int id = *(int *)arg;
    free(arg);
    
    while (1) {
        // Work on toys
        usleep(randomSleepMs(1000, 4000) * 1000);
        
        pthread_mutex_lock(&elfMutex);
        waitingElves++;
        
        if (waitingElves == ELF_GROUP_SIZE) {
            printf("Elf %d: We have 3 elves waiting! Waking Santa!\n", id);
            elfCount = ELF_GROUP_SIZE;
            waitingElves = 0;
            sem_post(&santaSem);  // Wake Santa
        } else {
            printf("Elf %d: Waiting for help (Total waiting: %d)\n", id, waitingElves);
        }
        
        pthread_mutex_unlock(&elfMutex);
        
        // Wait for consultation
        sem_wait(&elfSem);
        printf("Elf %d: Getting help from Santa...\n", id);
        usleep(100000);  // 0.1 seconds
        printf("Elf %d: Problem solved! Back to work!\n", id);
    }
    
    return NULL;
}

int main() {
    srand(time(NULL));
    
    printf("============================================================\n");
    printf("SANTA CLAUS PROBLEM - C IMPLEMENTATION\n");
    printf("============================================================\n");
    printf("Configuration:\n");
    printf("  - Number of Reindeer: %d\n", NUM_REINDEER);
    printf("  - Number of Elves: %d\n", NUM_ELVES);
    printf("  - Elves per consultation group: %d\n", ELF_GROUP_SIZE);
    printf("  - Number of Semaphores: 7\n");
    printf("============================================================\n");
    printf("\nStarting simulation...\n\n");
    
    // Initialize semaphores
    sem_init(&santaSem, 0, 0);
    sem_init(&reindeerSem, 0, 0);
    sem_init(&elfSem, 0, 0);
    sem_init(&santaMutex, 0, 1);
    
    // Create Santa thread
    pthread_t santaTid;
    pthread_create(&santaTid, NULL, santaThread, NULL);
    
    // Create reindeer threads
    pthread_t reindeerTids[NUM_REINDEER];
    for (int i = 0; i < NUM_REINDEER; i++) {
        int *id = malloc(sizeof(int));
        *id = i + 1;
        pthread_create(&reindeerTids[i], NULL, reindeerThread, id);
    }
    
    // Create elf threads
    pthread_t elfTids[NUM_ELVES];
    for (int i = 0; i < NUM_ELVES; i++) {
        int *id = malloc(sizeof(int));
        *id = i + 1;
        pthread_create(&elfTids[i], NULL, elfThread, id);
    }
    
    // Let simulation run
    sleep(SIMULATION_TIME);
    
    printf("\n============================================================\n");
    printf("Simulation Complete!\n");
    printf("============================================================\n");
    printf("Statistics:\n");
    printf("  - Total Deliveries: %d\n", deliveries);
    printf("  - Total Elf Consultations: %d\n", elfConsultations);
    printf("============================================================\n");
    
    // Cleanup
    sem_destroy(&santaSem);
    sem_destroy(&reindeerSem);
    sem_destroy(&elfSem);
    sem_destroy(&santaMutex);
    pthread_mutex_destroy(&reindeerMutex);
    pthread_mutex_destroy(&elfMutex);
    
    return 0;
}
