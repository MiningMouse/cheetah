package com.zhaijiong.stock.scheduler;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zhaijiong.stock.Context;
import com.zhaijiong.stock.Stock;
import com.zhaijiong.stock.common.Pair;
import com.zhaijiong.stock.common.Utils;
import com.zhaijiong.stock.dao.StockDB;
import com.zhaijiong.stock.datasource.MinuteStockDataCollecter;
import com.zhaijiong.stock.datasource.StockListFetcher;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.util.List;

import static com.zhaijiong.stock.common.Constants.BISNESS_DATA_FORMAT;

/**
 * author: xuqi.xq
 * mail: xuqi.xq@alibaba-inc.com
 * date: 15-8-12.
 */
public class StockMinuteDataDownloadJob implements Job{

    @Override
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        String type = jobContext.getJobDetail().getJobDataMap().getString("type");
        Preconditions.checkNotNull(type);

        String starttime = jobContext.getJobDetail().getJobDataMap().getString("starttime");
        String stoptime = jobContext.getJobDetail().getJobDataMap().getString("stoptime");
        if(Strings.isNullOrEmpty(starttime)){
            starttime = Utils.getNow(BISNESS_DATA_FORMAT);
        }
        if(Strings.isNullOrEmpty(stoptime)){
            starttime = Utils.getYesterday(BISNESS_DATA_FORMAT);
            stoptime = Utils.getTomorrow(BISNESS_DATA_FORMAT);
        }

        try {
            Context context = new Context();
            StockDB stockDB = new StockDB(context);
            StockListFetcher stockListFetcher = new StockListFetcher();
            List<Pair<String, String>> stockList = stockListFetcher.getStockList();
            for(Pair<String,String> stock:stockList){
                MinuteStockDataCollecter collecter = new MinuteStockDataCollecter(starttime,stoptime,type);
                List<Stock> stocks = collecter.collect(stock.getVal());
                if("5".equals(type)){
                    stockDB.saveStock5MinData(stocks);
                }else if("15".equals(type)){
                    stockDB.saveStock15MinData(stocks);
                }else if("30".equals(type)){
                    stockDB.saveStock30MinData(stocks);
                }else if("60".equals(type)){
                    stockDB.saveStock60MinData(stocks);
                }
            }
            context.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
