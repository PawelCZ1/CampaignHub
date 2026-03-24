package pl.pawelcz.campaignHub.campaign.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;

public interface EmeraldAccountRepository extends JpaRepository<EmeraldAccount, UUID> {
    EmeraldAccount findTopByOrderByIdAsc();
}
