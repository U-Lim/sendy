package com.witchdelivery.messageapp.global.config;

import com.witchdelivery.messageapp.domain.message.entity.Message;
import com.witchdelivery.messageapp.domain.message.repository.MessageRepository;
import com.witchdelivery.messageapp.domain.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class BatchConfig {   // created_at이 7일 이상이고 messageSaved가 false 인 경우에만 삭제 하도록 설계

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessageService messageService;

    @Bean
    public Job job() {

        Job job = jobBuilderFactory.get("job")
                .start(step1())
                .build();

        return job;
    }

    private Step step1() {
        return stepBuilderFactory.get("step1")
                .tasklet(((contribution, chunkContext) -> {        // 실행 할 작업을 지정하기 위한 tasklet() 메서드 호출
                    log.info("Step1");             // batch 프레임웍에서 제공되는 매개변수 contribution , chunkContext 이다. 단계의 실행을 제어하는데 사용한다.
                                                    // 단계가 시작되었다는 메시지 log.info
                    List<Message> limitedMessages = messageRepository.selectLimitedMessage();

                    if (limitedMessages != null && !limitedMessages.isEmpty()) {          // null이 아니거나 비어 있지 않은지 확인 (DB에 일치하는 값이 없는 경우에 NullPointerException 방지)
                        for (Message message : limitedMessages) {
                            if (!message.isMessageSaved()) {                              // messageSaved가 false이면 delete
                                messageService.deleteMessage(message.getMessageId());
                            }
                        }
                    }
                    return RepeatStatus.FINISHED;         // 반환, 생략 시 무한반복됨
                })
                )
                .build();         // Step 객체를 생성하기 위해 호출된다. Step 객체는 step1() 메서드에 의해 반환되고 Spring Batch 작업에 의해 실행된다.
    }

}
