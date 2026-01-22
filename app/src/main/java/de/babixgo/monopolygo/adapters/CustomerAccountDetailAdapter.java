package de.babixgo.monopolygo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.models.CustomerAccount;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying customer accounts in detail view
 * Shows account name, services, and backup status
 */
public class CustomerAccountDetailAdapter extends RecyclerView.Adapter<CustomerAccountDetailAdapter.ViewHolder> {
    
    private List<CustomerAccount> accounts = new ArrayList<>();
    private OnAccountClickListener listener;
    
    public interface OnAccountClickListener {
        void onAccountClick(CustomerAccount account);
    }
    
    public CustomerAccountDetailAdapter(OnAccountClickListener listener) {
        this.listener = listener;
    }
    
    public void setAccounts(List<CustomerAccount> accounts) {
        this.accounts = accounts != null ? accounts : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_customer_account_detail, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerAccount account = accounts.get(position);
        holder.bind(account);
    }
    
    @Override
    public int getItemCount() {
        return accounts.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIngameName, tvFriendCode, tvServices, tvBackupStatus;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvIngameName = itemView.findViewById(R.id.tv_ingame_name);
            tvFriendCode = itemView.findViewById(R.id.tv_friend_code);
            tvServices = itemView.findViewById(R.id.tv_services);
            tvBackupStatus = itemView.findViewById(R.id.tv_backup_status);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAccountClick(accounts.get(position));
                }
            });
        }
        
        void bind(CustomerAccount account) {
            tvIngameName.setText(account.getIngameName() != null ? account.getIngameName() : "Unbekannt");
            tvFriendCode.setText(account.getFriendCode() != null ? account.getFriendCode() : "-");
            tvServices.setText(account.getServicesDisplay());
            tvBackupStatus.setText(account.getBackupDateDisplay());
        }
    }
}
