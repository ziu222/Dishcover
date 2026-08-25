package com.dishcover.user.security;

import org.junit.jupiter.api.Test;

import static com.dishcover.user.security.LoginAttemptTracker.Status.LOCKED;
import static com.dishcover.user.security.LoginAttemptTracker.Status.NEEDS_CAPTCHA;
import static com.dishcover.user.security.LoginAttemptTracker.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginAttemptTrackerTest {

    private final LoginAttemptTracker tracker = new LoginAttemptTracker();

    @Test
    void unknownEmailIsOk() {
        assertEquals(OK, tracker.status("never@seen.com"));
    }

    @Test
    void staysOkBelowCaptchaThreshold() {
        String email = "a@b.com";
        for (int i = 0; i < LoginAttemptTracker.CAPTCHA_THRESHOLD - 1; i++) {
            tracker.recordFailure(email);
        }
        assertEquals(OK, tracker.status(email));
    }

    @Test
    void needsCaptchaAtThreshold() {
        String email = "a@b.com";
        for (int i = 0; i < LoginAttemptTracker.CAPTCHA_THRESHOLD; i++) {
            tracker.recordFailure(email);
        }
        assertEquals(NEEDS_CAPTCHA, tracker.status(email));
    }

    @Test
    void staysNeedsCaptchaBelowLockThreshold() {
        String email = "a@b.com";
        for (int i = 0; i < LoginAttemptTracker.LOCK_THRESHOLD - 1; i++) {
            tracker.recordFailure(email);
        }
        assertEquals(NEEDS_CAPTCHA, tracker.status(email));
    }

    @Test
    void lockedAtLockThreshold() {
        String email = "a@b.com";
        for (int i = 0; i < LoginAttemptTracker.LOCK_THRESHOLD; i++) {
            tracker.recordFailure(email);
        }
        assertEquals(LOCKED, tracker.status(email));
    }

    @Test
    void staysLockedEvenAfterMoreFailures() {
        String email = "a@b.com";
        for (int i = 0; i < LoginAttemptTracker.LOCK_THRESHOLD + 3; i++) {
            tracker.recordFailure(email);
        }
        assertEquals(LOCKED, tracker.status(email));
    }

    @Test
    void resetClearsHistoryImmediately() {
        String email = "a@b.com";
        for (int i = 0; i < LoginAttemptTracker.LOCK_THRESHOLD; i++) {
            tracker.recordFailure(email);
        }
        assertEquals(LOCKED, tracker.status(email));

        tracker.reset(email);

        assertEquals(OK, tracker.status(email));
    }

    @Test
    void emailsAreTrackedIndependently() {
        String locked = "locked@b.com";
        String clean = "clean@b.com";
        for (int i = 0; i < LoginAttemptTracker.LOCK_THRESHOLD; i++) {
            tracker.recordFailure(locked);
        }

        assertEquals(LOCKED, tracker.status(locked));
        assertEquals(OK, tracker.status(clean));
    }
}
