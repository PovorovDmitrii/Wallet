package com.wallet.WalletApp.service;
import com.wallet.WalletApp.exception.InsufficientFundsException;
import com.wallet.WalletApp.exception.InvalidOperationException;
import com.wallet.WalletApp.exception.WalletNotFoundException;
import com.wallet.WalletApp.repository.WalletRepository;

import com.wallet.WalletApp.model.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Retryable(
            value = { org.springframework.dao.OptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void updateBalance(UUID walletId, String operationType, BigDecimal amount) {
        logger.info("Updating balance for wallet: {}", walletId);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if ("DEPOSIT".equals(operationType)) {
            wallet.setBalance(wallet.getBalance().add(amount));
        } else if ("WITHDRAW".equals(operationType)) {
            if (wallet.getBalance().compareTo(amount) < 0) {
                logger.warn("Insufficient funds for wallet: {}", walletId);
                throw new InsufficientFundsException("Insufficient funds");
            }
            wallet.setBalance(wallet.getBalance().subtract(amount));
        } else {
            logger.warn("Invalid operation type: {}", operationType);
            throw new InvalidOperationException("Invalid operation type");
        }

        walletRepository.save(wallet);
        logger.info("Balance updated successfully for wallet: {}", walletId);
    }

    @Cacheable("walletBalance")
    public BigDecimal getBalance(UUID walletId) {
        logger.info("Fetching balance for wallet: {}", walletId);
        return walletRepository.findById(walletId)
                .map(Wallet::getBalance)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }
}
