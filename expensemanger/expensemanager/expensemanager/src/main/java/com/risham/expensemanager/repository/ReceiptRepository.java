package com.risham.expensemanager.repository;

import com.risham.expensemanager.entity.Receipt;
import com.risham.expensemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByUserOrderByUploadedAtDesc(User user);
}