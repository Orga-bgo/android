package de.babixgo.monopolygo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.models.CustomerActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying customer activity history
 * Shows activity type, description, category, and timestamp
 */
public class CustomerActivityAdapter extends RecyclerView.Adapter<CustomerActivityAdapter.ViewHolder> {
    
    private List<CustomerActivity> activities = new ArrayList<>();
    
    public void setActivities(List<CustomerActivity> activities) {
        this.activities = activities != null ? activities : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_customer_activity, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerActivity activity = activities.get(position);
        holder.bind(activity);
    }
    
    @Override
    public int getItemCount() {
        return activities.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvDescription, tvCategory, tvTimestamp;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_activity_icon);
            tvDescription = itemView.findViewById(R.id.tv_activity_description);
            tvCategory = itemView.findViewById(R.id.tv_activity_category);
            tvTimestamp = itemView.findViewById(R.id.tv_activity_timestamp);
        }
        
        void bind(CustomerActivity activity) {
            tvIcon.setText(activity.getActivityIcon());
            tvDescription.setText(activity.getDescription());
            tvCategory.setText(activity.getCategoryDisplay());
            tvTimestamp.setText(activity.getFormattedTimestamp());
        }
    }
}
