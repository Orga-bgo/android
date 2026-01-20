package de.babixgo.monopolygo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.models.Event;
import java.util.ArrayList;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
    
    private List<Event> events = new ArrayList<>();
    private OnEventClickListener listener;
    
    public interface OnEventClickListener {
        void onEventClick(Event event);
        void onEditClick(Event event);
    }
    
    public EventListAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }
    
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }
    
    @Override
    public int getItemCount() {
        return events.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvDateRange, tvTeamCount, tvEdit;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tv_event_name);
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            tvTeamCount = itemView.findViewById(R.id.tv_team_count);
            tvEdit = itemView.findViewById(R.id.tv_edit);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEventClick(events.get(position));
                }
            });
            
            tvEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(events.get(position));
                }
            });
        }
        
        void bind(Event event) {
            tvEventName.setText(event.getName());
            tvDateRange.setText(event.getFormattedDateRange());
            tvTeamCount.setText("0 Teams"); // TODO: Count teams
        }
    }
}
