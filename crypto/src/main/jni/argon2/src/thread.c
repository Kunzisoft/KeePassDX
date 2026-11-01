/*
 * Argon2 reference source code package - reference C implementations
 *
 * Copyright 2015
 * Daniel Dinu, Dmitry Khovratovich, Jean-Philippe Aumasson, and Samuel Neves
 *
 * You may use this work under the terms of a Creative Commons CC0 1.0 
 * License/Waiver or the Apache Public License 2.0, at your option. The terms of
 * these licenses can be found at:
 *
 * - CC0 1.0 Universal : http://creativecommons.org/publicdomain/zero/1.0
 * - Apache 2.0        : http://www.apache.org/licenses/LICENSE-2.0
 *
 * You should have received a copy of both of these licenses along with this
 * software. If not, they may be obtained at the above URLs.
 */

#include "thread.h"
#if defined(_WIN32)
#include <windows.h>
#endif

int argon2_thread_create(argon2_thread_handle_t *handle,
                         argon2_thread_func_t func, void *args) {
    if (NULL == handle || func == NULL) {
        return -1;
    }
#if defined(_WIN32)
    *handle = _beginthreadex(NULL, 0, func, args, 0, NULL);
    return *handle != 0 ? 0 : -1;
#else
    return pthread_create(handle, NULL, func, args);
#endif
}

int argon2_thread_join(argon2_thread_handle_t handle) {
#if defined(_WIN32)
    if (WaitForSingleObject((HANDLE)handle, INFINITE) == WAIT_OBJECT_0) {
        return CloseHandle((HANDLE)handle) != 0 ? 0 : -1;
    }
    return -1;
#else
    return pthread_join(handle, NULL);
#endif
}

void argon2_thread_exit(void) {
#if defined(_WIN32)
    _endthreadex(0);
#else
    pthread_exit(NULL);
#endif
}

int argon2_barrier_init(argon2_barrier_t *barrier, unsigned int threshold) {
    if (barrier == NULL || threshold == 0) {
        return -1;
    }

    barrier->threshold = threshold;
    barrier->waiting = 0;
    barrier->generation = 0;
    barrier->aborted = 0;

#if defined(_WIN32)
    InitializeCriticalSection(&barrier->mutex);
    InitializeConditionVariable(&barrier->cond);
    return 0;
#else
    if (pthread_mutex_init(&barrier->mutex, NULL) != 0) {
        return -1;
    }
    if (pthread_cond_init(&barrier->cond, NULL) != 0) {
        pthread_mutex_destroy(&barrier->mutex);
        return -1;
    }
    return 0;
#endif
}

int argon2_barrier_wait(argon2_barrier_t *barrier) {
    unsigned int generation;
    int rc = 0;

    if (barrier == NULL) {
        return -1;
    }

#if defined(_WIN32)
    EnterCriticalSection(&barrier->mutex);
#else
    pthread_mutex_lock(&barrier->mutex);
#endif

    if (barrier->aborted) {
        rc = -1;
    } else if (++barrier->waiting == barrier->threshold) {
        /* Last one in opens the barrier. */
        barrier->waiting = 0;
        barrier->generation++;
#if defined(_WIN32)
        WakeAllConditionVariable(&barrier->cond);
#else
        pthread_cond_broadcast(&barrier->cond);
#endif
    } else {
        /* The generation guards against spurious wakeups and against a fast
           participant lapping the others into the next segment. */
        generation = barrier->generation;
        while (generation == barrier->generation && !barrier->aborted) {
#if defined(_WIN32)
            SleepConditionVariableCS(&barrier->cond, &barrier->mutex, INFINITE);
#else
            pthread_cond_wait(&barrier->cond, &barrier->mutex);
#endif
        }
        if (barrier->aborted) {
            rc = -1;
        }
    }

#if defined(_WIN32)
    LeaveCriticalSection(&barrier->mutex);
#else
    pthread_mutex_unlock(&barrier->mutex);
#endif
    return rc;
}

void argon2_barrier_abort(argon2_barrier_t *barrier) {
    if (barrier == NULL) {
        return;
    }

#if defined(_WIN32)
    EnterCriticalSection(&barrier->mutex);
    barrier->aborted = 1;
    WakeAllConditionVariable(&barrier->cond);
    LeaveCriticalSection(&barrier->mutex);
#else
    pthread_mutex_lock(&barrier->mutex);
    barrier->aborted = 1;
    pthread_cond_broadcast(&barrier->cond);
    pthread_mutex_unlock(&barrier->mutex);
#endif
}

void argon2_barrier_destroy(argon2_barrier_t *barrier) {
    if (barrier == NULL) {
        return;
    }

#if defined(_WIN32)
    DeleteCriticalSection(&barrier->mutex);
#else
    pthread_cond_destroy(&barrier->cond);
    pthread_mutex_destroy(&barrier->mutex);
#endif
}
