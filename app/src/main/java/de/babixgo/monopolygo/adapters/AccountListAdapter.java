// AccountListAdapter.java
package de.babixgo.monopolygo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.models.Account;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.ViewHolder> {
    
    private List<Account> accounts = new ArrayList<>();
    private OnAccountClickListener listener;
    
    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }
    
    public AccountListAdapter(OnAccountClickListener listener) {
        this.listener = listener;
    }
    
    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.bind(account);
    }
    
    @Override
    public int getItemCount() {
        return accounts.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastPlayed, tvSuspension, tvError;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_account_name);
            tvLastPlayed = itemView.findViewById(R.id.tv_last_played);
            tvSuspension = itemView.findViewById(R.id.tv_suspension);
            tvError = itemView.findViewById(R.id.tv_error);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAccountClick(accounts.get(position));
                }
            });
        }
        
        void bind(Account account) {
            tvName.setText(account.getName());
            tvLastPlayed.setText(formatDate(account.getLastPlayed()));
            tvSuspension.setText(account.getSuspensionSummary());
            tvError.setText(account.getErrorStatusText());
            
            // Color error text red if error exists
            if (account.isHasError()) {
                tvError.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.error_red));
            } else {
                tvError.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_dark));
            }
        }
        
        private String formatDate(String timestamp) {
            if (timestamp == null) return "---";
            
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.GERMAN);
                SimpleDateFormat outputFormat = new SimpleDateFormat(
                    "dd.MM.yyyy", Locale.GERMAN);
                Date date = inputFormat.parse(timestamp);
                return outputFormat.format(date);
            } catch (Exception e) {
                return timestamp.substring(0, Math.min(10, timestamp.length()));
            }
        }
    }
}
