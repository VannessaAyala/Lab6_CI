package ec.edu.espe.lab6_ci.service;

import ec.edu.espe.lab6_ci.dto.WalletResponse;
import ec.edu.espe.lab6_ci.model.Wallet;
import ec.edu.espe.lab6_ci.repository.WalletRepository;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.AssertionErrors;

import java.util.Optional;

public class WalletServiceTest {

    private WalletService walletService;
    private RiskClient riskClient;
    private WalletRepository walletRepository;

    //Arrange común de todas las pruebas
    @BeforeEach
    public void setUp() {
        walletRepository = Mockito.mock(WalletRepository.class);
        riskClient = Mockito.mock(RiskClient.class);
        walletService = new WalletService(walletRepository, riskClient);
    }

    @Test
    void createWallet_validData_shouldSaveAndReturnResponse() {
        //Arrange
        String email = "vannessa.ayala@espe.edu.ec";
        double balance = 150.00;

        Mockito.when(riskClient.isBlocked(email)).thenReturn(false);

        Mockito.when(walletRepository.existByOwnerEmail(email)).thenReturn(false);

        Mockito.when(walletRepository.save(ArgumentMatchers.any(Wallet.class)))
                .thenAnswer(i -> i.getArgument(0));

        //Act
        WalletResponse response =  walletService.createWallet(email, balance);

        //Assert
        AssertionErrors.assertNotNull("El Id del Wallet no debe ser Null", response.getWalletId());
        Assertions.assertEquals(150, response.getBalance());

        Mockito.verify(riskClient).isBlocked(email);
        Mockito.verify(walletRepository).existByOwnerEmail(email);
        Mockito.verify(walletRepository).save(ArgumentMatchers.any(Wallet.class));
    }

    @Test
    void createWallet_invalidData_shouldThrowException_andNotCallDependencies() {
        //Arrange
        String invalid = "vannessa.ayala-espe.edu.ec";
        double balance = 15.00;

        //Act + Assert
        Assertions.assertThrows(IllegalArgumentException.class, ()
                -> walletService.createWallet(invalid, balance));

        //No debe llamar a ninguna dependencia porque falla la validación
        Mockito.verifyNoInteractions(walletRepository, riskClient);
    }

    @Test
    void deposit_walletNotFound_shouldThrowException() {
        //Arrange
        String walletId = "no-existe";

        Mockito.when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        //Act + Assert
        IllegalStateException ex =  Assertions.assertThrows(IllegalStateException.class, ()
                -> walletService.deposit(walletId, 50));

        Assertions.assertEquals("Wallet not found", ex.getMessage());
        Mockito.verify(walletRepository).findById(walletId);
        Mockito.verify(walletRepository, Mockito.never()).save(ArgumentMatchers.any(Wallet.class));
    }

    @Test
    void deposit_shouldUpdateBalance_andSave_usingCaptor() {
        //Arrange
        Wallet wallet = new Wallet("vannessa.ayala@espe.edu.ec", 100.00);
        String walletId = wallet.getId();

        Mockito.when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        Mockito.when(walletRepository.save(ArgumentMatchers.any(Wallet.class)))
                .thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);

        //Act
        double newBallance = walletService.deposit(walletId, 30.00);

        Mockito.verify(walletRepository).save(captor.capture());
        Wallet saved = captor.getValue();
        Assertions.assertEquals(newBallance, saved.getBalance());
    }

    @Test
    void withdraw_insufficientFunds_shouldThrowException_andNotSave() {
        //Arrange
        Wallet wallet = new Wallet("vannessa.ayala@espe.edu.ec", 250.00);
        String walletId = wallet.getId();

        Mockito.when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        //Act + Assert
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, ()
                -> walletService.withdraw(walletId, 1000.00));

        Assertions.assertEquals("Insufficient funds", ex.getMessage());
        Mockito.verify(walletRepository,  Mockito.never()).save(ArgumentMatchers.any(Wallet.class));
    }
}
