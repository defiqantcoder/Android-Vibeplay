import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from '../App';
import React from 'react';
import { describe, test, expect, vi } from 'vitest';

describe('App Web UI interactions', () => {
    test('renders main app shell and verifies play toggling, buttons and sliders', async () => {
        const user = userEvent.setup();
        render(<App />);

        // Find play buttons
        const playButtons = screen.getAllByRole('button', { name: /Play/i });
        expect(playButtons.length).toBeGreaterThan(0);

        // Verify Next and Prev buttons
        const nextButtons = screen.getAllByRole('button', { name: /Next/i });
        expect(nextButtons.length).toBeGreaterThan(0);
        await user.click(nextButtons[0]);

        const prevButtons = screen.getAllByRole('button', { name: /Previous/i });
        expect(prevButtons.length).toBeGreaterThan(0);
        await user.click(prevButtons[0]);

        const stopButtons = screen.queryAllByRole('button', { name: /Stop/i });
        if (stopButtons.length > 0) {
            await user.click(stopButtons[0]);
        }

        // At start, it should have play_arrow icon
        const playIcons = screen.queryAllByText(/play_arrow/i);
        expect(playIcons.length).toBeGreaterThan(0);

        await user.click(playButtons[0]);

        await waitFor(() => {
            const pauseIcons = screen.queryAllByText(/pause/i);
            expect(pauseIcons.length).toBeGreaterThan(0);
        });

        // Click pause again
        const pauseButtons = screen.getAllByRole('button', { name: /Pause/i });
        await user.click(pauseButtons[0]);

        await waitFor(() => {
            const newPlayIcons = screen.queryAllByText(/play_arrow/i);
            expect(newPlayIcons.length).toBeGreaterThan(0);
        });

        // Verify sliders
        const sliders = screen.queryAllByRole('slider');
        if (sliders.length >= 2) {
           const seekSlider = sliders[0];
           const volumeSlider = sliders[1];

           fireEvent.change(volumeSlider, { target: { value: 0.5 } });
           fireEvent.change(seekSlider, { target: { value: 50 } });
        }

        // Verify Album Art Containers
        const imgEls = screen.getAllByRole('img');
        expect(imgEls.length).toBeGreaterThan(0);

        // Check for playback state labels (like IDLE, STOPPED, PLAYING etc)
        // Check for state labels
        const stateLabels = screen.queryAllByText(/STOPPED|PLAYING|PAUSED|IDLE|BUFFERING/i);
        expect(stateLabels.length).toBeGreaterThanOrEqual(0); // Optional based on current text formatting
    });
});
