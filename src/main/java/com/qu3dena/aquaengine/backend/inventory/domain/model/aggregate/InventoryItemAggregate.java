package com.qu3dena.aquaengine.backend.inventory.domain.model.aggregate;

import com.qu3dena.aquaengine.backend.inventory.domain.model.commands.AdjustInventoryCommand;
import com.qu3dena.aquaengine.backend.inventory.domain.model.commands.CreateInventoryItemCommand;
import com.qu3dena.aquaengine.backend.inventory.domain.model.commands.ReleaseInventoryCommand;
import com.qu3dena.aquaengine.backend.inventory.domain.model.commands.ReserveInventoryCommand;
import com.qu3dena.aquaengine.backend.inventory.domain.model.events.StockLowEvent;
import com.qu3dena.aquaengine.backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.qu3dena.aquaengine.backend.shared.domain.model.valuobjects.Money;
import com.qu3dena.aquaengine.backend.shared.domain.model.valuobjects.Quantity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Entity
@NoArgsConstructor
@Table(name = "inventory_items")
@EqualsAndHashCode(callSuper = true)
public class InventoryItemAggregate extends AuditableAbstractAggregateRoot<InventoryItemAggregate> {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Money price;

    @Embedded
    @AttributeOverrides(
            @AttributeOverride(name = "amount", column = @Column(name = "quantity_on_hand", nullable = false))
    )
    private Quantity quantityOnHand;

    @Embedded
    @AttributeOverrides(
            @AttributeOverride(name = "amount", column = @Column(name = "reserved_quantity", nullable = false))
    )
    private Quantity reservedQuantity;

    @Column(nullable = false)
    private int threshold;

    public InventoryItemAggregate(Long userId, String name, Money price, Quantity quantityOnHand, int threshold) {
        this.userId = userId;
        this.name = name;
        this.price = price;
        this.quantityOnHand = quantityOnHand;
        this.reservedQuantity = new Quantity(0);
        this.threshold = threshold;
    }

    public static InventoryItemAggregate create(CreateInventoryItemCommand command) {

        if (command.quantityOnHand() < 0)
            throw new IllegalArgumentException("Available quantity cannot be negative");

        return new InventoryItemAggregate(
                command.userId(),
                command.name(),
                command.price(),
                new Quantity(command.quantityOnHand()),
                command.threshold()
        );
    }

    /**
     * Adjusts the stock for the inventory item.
     *
     * @param command the command containing the adjustment amount
     * @return an Optional containing a StockLowEvent if the stock falls below the threshold, otherwise an empty Optional
     * @throws IllegalArgumentException if the new quantity is negative
     */
    public Optional<StockLowEvent> adjustStock(AdjustInventoryCommand command) {

        var newQuantity = quantityOnHand.amount() + command.adjustBy();

        if (newQuantity < 0)
            throw new IllegalArgumentException("Insufficient stock to adjust");

        quantityOnHand = new Quantity(newQuantity);

        if (quantityOnHand.amount() <= threshold)
            return Optional.of(new StockLowEvent(this.getId(), name, quantityOnHand.amount()));

        return Optional.empty();
    }

    public Optional<StockLowEvent> reserveStock(ReserveInventoryCommand command) {

        var toReserve = command.quantityToReserve();
        var available = getQuantityOnHand() - getReservedQuantity();

        if (available < toReserve)
            throw new IllegalArgumentException("Not enough stock to reserve for product " + name);

        reservedQuantity = new Quantity(getReservedQuantity() + toReserve);

        if (getQuantityOnHand() - getReservedQuantity() <= threshold)
            return Optional.of(new StockLowEvent(this.getId(), name, getQuantityOnHand()));

        return Optional.empty();
    }

    public void releaseStock(ReleaseInventoryCommand command) {
        var toRelease = command.quantityToRelease();

        if (getReservedQuantity() < toRelease)
            throw new IllegalArgumentException("Cannot release more stock than reserved for product " + name);

        reservedQuantity = new Quantity(getReservedQuantity() - toRelease);
    }

    public int getQuantityOnHand() {
        return quantityOnHand.amount();
    }

    public int getReservedQuantity() {
        return reservedQuantity.amount();
    }
}
