package net.oldschoolminecraft.dvr;

import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.WeatherListener;

public class WeatherHandler extends WeatherListener
{
    public void onWeatherChange(WeatherChangeEvent event)
    {
        if (event.toWeatherState() && !DVRedux.getInstance().shouldWeatherBeOn)
        {
            System.out.println("[DVRedux] Weather change event was cancelled as it was not voted on.");
            event.setCancelled(true);
            return;
        }

        // we just changed it, no further changes should be accepted until the next vote
        if (event.toWeatherState())
            DVRedux.getInstance().shouldWeatherBeOn = false;
    }
}
