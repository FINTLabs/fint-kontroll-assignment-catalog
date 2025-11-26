package no.fintlabs.device.azure;

import no.fintlabs.device.AzureStatus;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.KontrollStatus;
import no.fintlabs.device.azureInfo.DeviceAzureInfo;
import no.fintlabs.device.azureInfo.DeviceAzureInfoRepository;
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
    private DeviceAzureInfoRepository deviceAzureInfoRepository;

    @Mock
    private DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @InjectMocks
    private AzureAdGroupDeviceMembershipConsumer consumer;

    private UUID deviceAzureId;
    private UUID resourceAzureId;
    private DeviceAzureInfo deviceAzureInfo;

    @BeforeEach
    void setUp() {
        deviceAzureId = UUID.randomUUID();
        resourceAzureId = UUID.randomUUID();
        deviceAzureInfo = DeviceAzureInfo.builder()
                .deviceAzureId(deviceAzureId)
                .resourceAzureId(resourceAzureId)
                .build();
    }

    @Test
    void processGroupMembership_shouldSetStatusConfirmed_whenCodeIsAddedAndAlreadyActive() {
        AzureAdDeviceGroupMembership payload = createPayload(AzureReturnCode.ADDED);
        deviceAzureInfo.setKontrollStatus(KontrollStatus.ACTIVE);
        
        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceAzureInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAzureInfoRepository).save(argThat(info -> info.getAzureStatus() == AzureStatus.CONFIRMED));
        verifyNoInteractions(deviceAssigmentEntityProducerService);
    }

    @Test
    void processGroupMembership_shouldPublish_whenCodeIsAddedAndNotActive() {
        AzureAdDeviceGroupMembership payload = createPayload(AzureReturnCode.ADDED);
        deviceAzureInfo.setKontrollStatus(KontrollStatus.INACTIVE);

        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceAzureInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAssigmentEntityProducerService).publish(deviceAzureInfo);
        verify(deviceAzureInfoRepository, never()).save(any());
    }

    @Test
    void processGroupMembership_shouldSetStatusDeletionConfirmed_whenCodeIsRemovedAndAlreadyInactive() {
        AzureAdDeviceGroupMembership payload = createPayload(AzureReturnCode.REMOVED);
        deviceAzureInfo.setKontrollStatus(KontrollStatus.INACTIVE);

        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceAzureInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAzureInfoRepository).save(argThat(info -> info.getAzureStatus() == AzureStatus.DELETION_CONFIRMED));
    }

    @Test
    void processGroupMembership_shouldSetStatusNeedsRepublish_whenCodeIsFailed() {
        AzureAdDeviceGroupMembership payload = createPayload(AzureReturnCode.FAILED);

        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceAzureInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAzureInfoRepository).save(argThat(info -> info.getAzureStatus() == AzureStatus.NEEDS_REPUBLISH));
    }

    @Test
    void processGroupMembership_shouldSetStatusError_whenCodeIsError() {
        AzureAdDeviceGroupMembership payload = createPayload(AzureReturnCode.ERROR);

        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId))
                .thenReturn(Optional.of(deviceAzureInfo));

        consumer.processGroupMembership(new ConsumerRecord<>("topic", 0, 0L, "key", payload));

        verify(deviceAzureInfoRepository).save(argThat(info -> info.getAzureStatus() == AzureStatus.ERROR));
    }

    private AzureAdDeviceGroupMembership createPayload(AzureReturnCode code) {
        return AzureAdDeviceGroupMembership.builder()
                .azureDeviceRef(deviceAzureId)
                .azureResourceRef(resourceAzureId)
                .code(code)
                .build();
    }
}