package main;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.io.File;
import java.io.FileWriter;

import com.jietong.rfid.uhf.dao.impl.Reader;
import com.jietong.rfid.uhf.service.CallBack;
import com.jietong.rfid.uhf.service.ReaderService;
import com.jietong.rfid.uhf.service.impl.ReaderServiceImpl;

public class MainProgram{
	
	static String id_cabina;
	static String host;
	static LogFile log_file;
	static ApiServer api_server;
	static String last_epc_processed = "";
	
	// MAIN PROGRAM
	public static void main(String [] args) throws IOException, Throwable{
		
		//  MAIN CONFIG
		host = ReadConfigMain.getHost();
		id_cabina = ReadConfigMain.getIdCabina();
		
		// LOG FILE CONFIG
		log_file = ReadConfigMain.getLogFile();
		LogFile.verificarTamanio();
		
		// SERVER CONFIG
		api_server = ReadConfigMain.getIpDestino();
		ApiServer.setIdCabina(id_cabina);
		
		// SERVICIO LECTOR EN ATENA
		ReaderService readerService = new ReaderServiceImpl();
		Reader reader = new Reader();		
		
		//CONEXION SERVICIO
		reader = readerService.connect(host, 0);
		readerService.beginInvV2(reader, new CallBackData());

	    //readerService.disconnect(reader);
	}
	
	// CALLBACK ASIGNADO A LECTURA DE ANTENA
	public static class CallBackData implements CallBack {
		
		@Override
		public void readData(String data, String rssi, String antNo,String deviceNo, String direction) {

			if( ! last_epc_processed.equals(data)) {
				
				//ACTUALIZA ULTIMO PROCESADO
				last_epc_processed = data;
				
				// TIME
				String right_now_time = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date());
				
				// SERVER SEND
				ApiServer.enviarLectura(data, right_now_time);

				//GUARDA EN LOG
				try {
					LogFile.guardarLectura(data, right_now_time);
				} catch (IOException e) {
					System.out.println("Error al guardar la lectura en el log local");
					e.printStackTrace();
				}
				

			}
		}
	}

}


