package de.babixgo.monopolygo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.babixgo.monopolygo.R;

/**
 * Placeholder Fragment for Tycoon Racers
 * Full implementation in Phase 3
 */
public class TycoonRacersFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        // TODO: Implement in Phase 3
        // For now, return a simple placeholder view
        View view = new View(requireContext());
        view.setBackgroundColor(0xFFE9EEF2); // background_light color
        return view;
    }
}
