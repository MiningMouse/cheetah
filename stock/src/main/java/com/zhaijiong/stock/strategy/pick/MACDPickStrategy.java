package com.zhaijiong.stock.strategy.pick;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.zhaijiong.stock.common.Conditions;
import com.zhaijiong.stock.common.StockConstants;
import com.zhaijiong.stock.common.Utils;
import com.zhaijiong.stock.model.PeriodType;
import com.zhaijiong.stock.model.StockData;
import com.zhaijiong.stock.provider.Provider;
import com.zhaijiong.stock.strategy.PickStrategy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * author: eryk
 * mail: xuqi86@gmail.com
 * date: 15-10-18.
 * 结合日线和15分钟k线选股
 */
public class MACDPickStrategy implements PickStrategy {

    private int timeRange = 5;
    private PeriodType type;

    public MACDPickStrategy(int timeRange,PeriodType type){
        this.timeRange = timeRange;
        this.type = type;
    }

    /**
     * 策略判断步骤说明:
     * 1.判断日线级别macd最近n天是否处于金叉状态,并且红柱持续放大
     * 2.判断最近n根15分钟数据是否处于金叉状态
     *
     * @return
     */
    @Override
    public boolean pick(String symbol) {
        List<StockData> stockDataList;
        switch (type){
            case FIVE_MIN:
                stockDataList = Lists.newArrayList(Provider.minuteData(symbol, "5"));
                break;
            case FIFTEEN_MIN:
                stockDataList = Lists.newArrayList(Provider.minuteData(symbol, "15"));
                break;
            case THIRTY_MIN:
                stockDataList = Lists.newArrayList(Provider.minuteData(symbol, "30"));
                break;
            case SIXTY_MIN:
                stockDataList = Lists.newArrayList(Provider.minuteData(symbol, "60"));
                break;
            case DAY:
                stockDataList = Lists.newArrayList(Provider.dailyData(symbol,false));
                break;
            default:
                stockDataList = Lists.newArrayList(Provider.dailyData(symbol,false));
        }

        if (isGoldenCrossIn(stockDataList, timeRange)) {
            return true;
        }
        return false;
    }

    /**
     * 判断最近n个时间周期内是否出现金叉
     *
     * @param period
     * @return 如果是返回true
     */
    public boolean isGoldenCrossIn(List<StockData> stockDataList, int period) {
        int count = stockDataList.size();
        for (int i = count - 1; i > 0; i--) {
            StockData stockData = stockDataList.get(i);
            Double cross = stockData.get(StockConstants.MACD_CROSS);
            if (cross != null && count - i <= period && cross == 1)
                return true;
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        MACDPickStrategy dayMacdStrategy = new MACDPickStrategy(7,PeriodType.DAY);
        MACDPickStrategy minute15MacdStrategy = new MACDPickStrategy(8,PeriodType.FIFTEEN_MIN);
        MACDPickStrategy minute5MacdStrategy = new MACDPickStrategy(16,PeriodType.FIVE_MIN);

        List<String> stockList = Provider.tradingStockList();
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(stockList.size());

        for (String symbol : stockList) {
            pool.execute(() -> {
                if (dayMacdStrategy.pick(symbol)) {
                    if (minute15MacdStrategy.pick(symbol)) {
                        if(minute5MacdStrategy.pick(symbol)){
                            System.out.println(symbol);
                        }
                    }
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        Utils.closeThreadPool(pool);
        System.out.println(stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
