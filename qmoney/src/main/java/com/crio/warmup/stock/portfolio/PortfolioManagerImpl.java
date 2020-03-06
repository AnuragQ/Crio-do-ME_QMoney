package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {



  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Now we want to convert our code into a module, so we will not call it from main anymore.
  // Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and make sure that it
  // follows the method signature.
  // Logic to read Json file and convert them into Objects will not be required further as our
  // clients will take care of it, going forward.
  // Test your code using Junits provided.
  // Make sure that all of the tests inside PortfolioManagerTest using command below -
  // ./gradlew test --tests PortfolioManagerTest
  // This will guard you against any regressions.
  // run ./gradlew build in order to test yout code, and make sure that
  // the tests and static code quality pass.

  // CHECKSTYLE:OFF



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo thirdparty APIs to a separate function.
  // It should be split into fto parts.
  // Part#1 - Prepare the Url to call Tiingo based on a template constant,
  // by replacing the placeholders.
  // Constant should look like
  // https://api.tiingo.com/tiingo/daily/<ticker>/prices?startDate=?&endDate=?&token=?
  // Where ? are replaced with something similar to <ticker> and then actual url produced by
  // replacing the placeholders with actual parameters.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    String uri = buildUri(symbol, from, to);
    ObjectMapper mapper = getObjectMapper();
    String result = restTemplate.getForObject(uri, String.class);
    List<Candle> collection = null;
    try {
      collection = mapper.readValue(result, new TypeReference<ArrayList<Candle>>() {
      });
    } catch (JsonMappingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }


    return collection;
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uriTemplate =
        "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?" + "startDate=" + startDate.toString()
            + "&endDate=" + endDate.toString() + "&token=0e33b35ed0c8ae45d1ec18fcae2104bbf0106b68";
    return uriTemplate;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {
    List<AnnualizedReturn> list2 = new ArrayList<AnnualizedReturn>();

    for (PortfolioTrade i : portfolioTrades) {
      LocalDate startDate = i.getPurchaseDate();
      if (startDate.isAfter(endDate)) {
        throw new RuntimeException();
      }
      String symbol = i.getSymbol();



      List<Candle> collection = null;
      try {
        collection = getStockQuote(symbol, startDate, endDate);
      } catch (JsonProcessingException e) {
        // TODO Auto-generated catch block
        throw new RuntimeException();
      }


      Double closePrice = null;
      Double buyPrice = null;
      LocalDate closePriceDate = null;
      for (Candle t : collection) {
        if (closePrice == null || t.getDate().isEqual(endDate)
            || t.getDate().isAfter(closePriceDate)) {
          closePrice = t.getClose();
          closePriceDate = t.getDate();
        }
        if (t.getDate().isEqual((endDate))) {
          break;
        }
      }
      for (Candle t : collection) {
        if (t.getDate().isEqual(i.getPurchaseDate())) {
          buyPrice = t.getOpen();
          break;
        }
      }
      // System.out.println(last.getDate());
      if (closePrice != null && buyPrice != null) {
        AnnualizedReturn annualizedReturn =
            calculateAnnualizedReturns(endDate, i, buyPrice, closePrice);
        list2.add(annualizedReturn);
      }
    }
    Collections.sort(list2, getComparator());



    return list2;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    // System.out.println(trade.toString());
    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    // Double time = (endDate.getDayOfYear() -
    // trade.getPurchaseDate().getDayOfYear())/365.0;
    Double time = (double) ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    // if (time <= 0) {

    // }
    // System.out.println(endDate.getDayOfYear());
    // System.out.println();
    // if(time==0){
    // // return new AnnualizedReturn(trade.getSymbol(), totalReturn, totalReturn);
    // time=1;
    // }

    Double annualizedReturn = Math.pow((1 + totalReturn), 365.0 / (time)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }
}
