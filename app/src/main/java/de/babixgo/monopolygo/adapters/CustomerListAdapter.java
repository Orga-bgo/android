package de.babixgo.monopolygo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.models.Customer;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying customers in a RecyclerView
 */
public class CustomerListAdapter extends RecyclerView.Adapter<CustomerListAdapter.ViewHolder> {
    
    private List<Customer> customers = new ArrayList<>();
    private OnCustomerClickListener listener;
    
    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }
    
    public CustomerListAdapter(OnCustomerClickListener listener) {
        this.listener = listener;
    }
    
    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_customer, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = customers.get(position);
        holder.bind(customer);
    }
    
    @Override
    public int getItemCount() {
        return customers.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvServices, tvAccountCount;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_customer_name);
            tvServices = itemView.findViewById(R.id.tv_services);
            tvAccountCount = itemView.findViewById(R.id.tv_account_count);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCustomerClick(customers.get(position));
                }
            });
        }
        
        void bind(Customer customer) {
            tvName.setText(customer.getName());
            tvServices.setText(customer.getServicesDisplay());
            tvAccountCount.setText(String.valueOf(customer.getAccountCount()));
        }
    }
}
