package com.wallet.WalletApp.controller;
import com.wallet.WalletApp.dto.WalletOperationRequest;
import com.wallet.WalletApp.exception.InsufficientFundsException;
import com.wallet.WalletApp.exception.WalletNotFoundException;
import com.wallet.WalletApp.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
public class WalletControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    public void testUpdateBalance() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest();
        request.setWalletId(UUID.randomUUID());
        request.setOperationType("DEPOSIT");
        request.setAmount(BigDecimal.valueOf(1000));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType("application/json")
                        .content("{\"walletId\":\"" + request.getWalletId() + "\",\"operationType\":\"DEPOSIT\",\"amount\":1000}"))
                .andExpect(status().isOk());

        verify(walletService, times(1)).updateBalance(request.getWalletId(), request.getOperationType(), request.getAmount());
    }

    @Test
    public void testGetBalance() throws Exception {
        UUID walletId = UUID.randomUUID();
        when(walletService.getBalance(walletId)).thenReturn(BigDecimal.valueOf(1000));

        mockMvc.perform(get("/api/v1/wallet/" + walletId))
                .andExpect(status().isOk())
                .andExpect(content().string("1000"));

        verify(walletService, times(1)).getBalance(walletId);
    }

    @Test
    public void testWalletNotFound() throws Exception {
        UUID walletId = UUID.randomUUID();
        when(walletService.getBalance(walletId)).thenThrow(new WalletNotFoundException("Wallet not found"));

        mockMvc.perform(get("/api/v1/wallet/" + walletId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"))
                .andExpect(jsonPath("$.errorCode").value("WALLET_NOT_FOUND"));
    }


    @Test
    public void testInsufficientFunds() throws Exception {
        UUID walletId = UUID.randomUUID();
        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(walletService).updateBalance(walletId, "WITHDRAW", BigDecimal.valueOf(1000));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType("application/json")
                        .content("{\"walletId\":\"" + walletId + "\",\"operationType\":\"WITHDRAW\",\"amount\":1000}"))
                .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString())) // Логируем ответ
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient funds"))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    public void testInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/wallet")
                        .contentType("application/json")
                        .content("{\"walletId\":\"invalid-uuid\",\"operationType\":\"INVALID\",\"amount\":-100}"))
                .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
    }
}
