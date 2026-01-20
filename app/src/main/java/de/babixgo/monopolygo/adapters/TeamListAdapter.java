package de.babixgo.monopolygo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.models.Team;
import java.util.ArrayList;
import java.util.List;

public class TeamListAdapter extends RecyclerView.Adapter<TeamListAdapter.ViewHolder> {
    
    private List<Team> teams = new ArrayList<>();
    private OnTeamClickListener listener;
    
    public interface OnTeamClickListener {
        void onTeamClick(Team team);
    }
    
    public TeamListAdapter(OnTeamClickListener listener) {
        this.listener = listener;
    }
    
    public void setTeams(List<Team> teams) {
        this.teams = teams;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_team, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Team team = teams.get(position);
        holder.bind(team);
    }
    
    @Override
    public int getItemCount() {
        return teams.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTeamName, tvCustomerName;
        TextView tvSlot1, tvSlot2, tvSlot3;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvTeamName = itemView.findViewById(R.id.tv_team_name);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvSlot1 = itemView.findViewById(R.id.tv_slot_1);
            tvSlot2 = itemView.findViewById(R.id.tv_slot_2);
            tvSlot3 = itemView.findViewById(R.id.tv_slot_3);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTeamClick(teams.get(position));
                }
            });
        }
        
        void bind(Team team) {
            tvTeamName.setText(team.getName());
            tvCustomerName.setText(team.getCustomerId() != null ? "Kunde" : "---");
            tvSlot1.setText(team.getSlot1Name() != null ? team.getSlot1Name() : "---");
            tvSlot2.setText(team.getSlot2Name() != null ? team.getSlot2Name() : "---");
            tvSlot3.setText(team.getSlot3Name() != null ? team.getSlot3Name() : "---");
        }
    }
}
