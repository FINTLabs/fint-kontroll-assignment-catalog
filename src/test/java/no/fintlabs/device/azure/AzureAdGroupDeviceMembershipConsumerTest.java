package no.fintlabs.device.azure;

import no.fintlabs.device.EntraStatus;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.KontrollStatus;
import no.fintlabs.device.entraInfo.DeviceEntraInfo;
import no.fintlabs.device.entraInfo.DeviceEntraInfoRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureAdGroupDeviceMembershipConsumerTest {

    @Mock
    private DeviceEntraInfoRepository deviceEntraInfoRepository;

    @Mock
    private DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @InjectMocks
    private AzureAdGroupDeviceMembershipConsumer consumer;

    private UUID deviceAzureId;
    private UUID resourceAzureId;
    private DeviceEntraInfo deviceEntraInfo;

    @BeforeEach
    void setUp() {
        deviceAzureId = UUID.randomUUID();
        resourceAzureId = UUID.randomUUID();
        deviceEntraInfo = DeviceEntraInfo.builder()
                .deviceAzureId(deviceAzureId)
                .resourceAzureId(resourceAzureId)
                .build();
    }

    @Test
    void processGroupMembership_shouldSetStatusConfirmed_whenCodeIsAddedAndAlreadyActive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ADDED);
        deviceEntraInfo.setKontrollStatus(KontrollStatus.ACTIVE);
        
        when(deviceEntraInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraInfoRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    @Test
    void processGroupMembership_shouldPublish_whenCodeIsAddedAndNotActive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ADDED);
        deviceEntraInfo.setKontrollStatus(KontrollStatus.INACTIVE);

        when(deviceEntraInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAssigmentEntityProducerService).publish(deviceEntraInfo);
        verify(deviceEntraInfoRepository, never()).save(any());
    }

    @Test
    void processGroupMembership_shouldSetStatusDeletionConfirmed_whenCodeIsRemovedAndAlreadyInactive() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.REMOVED);
        deviceEntraInfo.setKontrollStatus(KontrollStatus.INACTIVE);

        when(deviceEntraInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraInfoRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.DELETION_CONFIRMED));
    }

    @Test
    void processGroupMembership_shouldSetStatusNeedsRepublish_whenCodeIsFailed() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.FAILED);

        when(deviceEntraInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraInfoRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.NEEDS_REPUBLISH));
    }

    @Test
    void processGroupMembership_shouldSetStatusError_whenCodeIsError() {
        EntraDeviceGroupMembership payload = createPayload(EntraReturnCode.ERROR);

        when(deviceEntraInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceEntraInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceEntraInfoRepository).save(argThat(info -> info.getEntraStatus() == EntraStatus.ERROR));
    }

    private EntraDeviceGroupMembership createPayload(EntraReturnCode code) {
        return EntraDeviceGroupMembership.builder()
                .entraDeviceRef(deviceAzureId)
                .entraResourceRef(resourceAzureId)
                .code(code)
                .build();
    }
}