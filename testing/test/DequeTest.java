package io.file;

import org.junit.Test;
import taxonomy.Ranks;

import java.util.*;

/**
 * Created by alext on 4/2/14.
 */ //TODO remove this to test folder
public class DequeTest {
    @Test
    public void test(){
        final Ranks[] rdpFixRankRanks= {Ranks.superkingdom, Ranks.phylum, Ranks.c_lass, Ranks.order, Ranks.family, Ranks.genus, Ranks.species};
        final Deque<Ranks> orderOfRankAppearence=new ArrayDeque<>(Arrays.asList(rdpFixRankRanks));
        final Set<Ranks> rdpFixRankRanksSet = new HashSet<>(orderOfRankAppearence);

        for(int i=0;i<7;i++){
            System.out.println(orderOfRankAppearence.pollLast());
        }
    }

}
