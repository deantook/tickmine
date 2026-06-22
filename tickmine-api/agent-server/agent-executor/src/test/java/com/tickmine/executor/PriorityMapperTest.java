package com.tickmine.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PriorityMapperTest {

    @Test
    void mapsLow() {
        assertEquals(1, PriorityMapper.toTickTick("low"));
    }

    @Test
    void mapsMedium() {
        assertEquals(3, PriorityMapper.toTickTick("medium"));
    }

    @Test
    void mapsHigh() {
        assertEquals(5, PriorityMapper.toTickTick("high"));
    }

    @Test
    void mapsNull() {
        assertEquals(0, PriorityMapper.toTickTick(null));
    }

    @Test
    void mapsUnknown() {
        assertEquals(0, PriorityMapper.toTickTick("unknown"));
    }
}
