/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.streaming.pool.core.examples.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.streaming.pool.core.conf.EmbeddedPoolConfiguration;
import cern.streaming.pool.core.service.DiscoveryService;
import io.reactivex.subscribers.TestSubscriber;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { EmbeddedPoolConfiguration.class, IntegerStreamFactory.class })
public class IntegerExampleTest {

    @Autowired
    private DiscoveryService discovery;
    
    @Test
    public void test() throws InterruptedException {
        IntegerRangeId streamId = new IntegerRangeId(0, 10);
        
        TestSubscriber<Integer> subscriber = TestSubscriber.create();        
        
        discovery.discover(streamId).subscribe(subscriber);
        
        subscriber.await();
        
        List<Integer> values = subscriber.values();
        List<Integer> expectedValues = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        
        assertThat(values).hasSize(10).containsExactlyElementsOf(expectedValues);
    }

}
