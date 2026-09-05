package com.example;

/**
 * AIDL Interface for Termux Overlay API.
 * Allows Linux shell / Termux processes to control floating Android overlay windows via Binder IPC.
 */
interface IOverlayService {
    /**
     * Display a floating text overlay.
     */
    void showText(String text);

    /**
     * Hide and remove the currently visible overlay.
     */
    void hide();

    /**
     * Update the text inside the currently visible overlay without destroying the window.
     */
    void updateText(String text);

    /**
     * Display a floating interactive button overlay.
     * When clicked, broadcasts an intent or writes event to communication channel.
     */
    void showButton(String label);

    /**
     * Display an image from local filesystem path (e.g. /sdcard/image.png or Termux home).
     */
    void showImage(String path);

    /**
     * Query whether the overlay window is currently showing.
     */
    boolean isShowing();

    /**
     * Set the window position offset (x, y) relative to screen gravity.
     */
    void setPosition(int x, int y);

    /**
     * Set the overlay window transparency alpha (0.1f - 1.0f).
     */
    void setAlpha(float alpha);

    /**
     * Set custom text color (e.g. #00FF66 or green).
     */
    void setTextColor(String hexColor);

    /**
     * Set custom background color (e.g. #DD111827).
     */
    void setBackgroundColor(String hexColor);

    /**
     * Get the last clicked button or event label.
     */
    String getLastAction();
}
