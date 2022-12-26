package main;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import com.jietong.rfid.uhf.dao.impl.Reader;
import com.jietong.rfid.uhf.service.CallBack;
import com.jietong.rfid.uhf.service.ReaderService;
import com.jietong.rfid.uhf.service.impl.ReaderServiceImpl;

public class MainProgram{
	
	static String host;
	static LogFile log_file;
	static Mail mail;
	static ApiServer api_server;
	static String last_epc_processed = "";
	
	// MAIN PROGRAM
	public static void main(String [] args) throws IOException, Throwable{		
		 
		System.out.println("Version: 2.0"); 
		// SERVER CONFIG
		 api_server = ReadConfigMain.getServer();
		 //api_server = new ApiServer( "http://190.12.101.198:5000/","88","88","KxZVM@&0$SOx_88");
		 System.out.println("Apuntando al servidor:".concat(api_server.ip_destino));
		
		
		// LOG FILE CONFIG
		log_file = ReadConfigMain.getLogFile();
		//log_file = new LogFile(" ",Integer.parseInt("20"));
		log_file.verificarTamanio();
		
		
		//SERVICIO LECTOR EN ATENA
		host = ReadConfigMain.getHost();
		ReaderService readerService = new ReaderServiceImpl();
		Reader reader = new Reader();		
		
		//CONEXION SERVICIO
		reader = readerService.connect(host, 0);
		readerService.beginInvV2(reader, new CallBackData());

	    //readerService.disconnect(reader);
		//  try{
		// 	// DEV
		// 	 test();
		//  } catch (Exception e) {
		// 	 e.printStackTrace();
		//  }
	}
	
	// TEST MAIN PROGRAM NO RFID READER
	public static void test() throws IOException, Throwable{		
		
		final String data = "000100000000000000000000";
		
		if( ! last_epc_processed.equals(data)) {
			
			//ACTUALIZA ULTIMO PROCESADO
			System.out.println("Nuevo EPC detectado:".concat(data));
			last_epc_processed = data;
			
			// TIME
			String right_now_time = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss").format(new Date());
			
			//SERVER SEND
			ApiServer.enviarLectura(last_epc_processed, right_now_time);
			
			//GUARDA EN LOG LOCAL
			log_file.guardarLectura(last_epc_processed, right_now_time); 
			
		}
	}
	
	// CALLBACK ASIGNADO A LECTURA DE ANTENA
	public static class CallBackData implements CallBack {
		
		@Override
		public void readData(String data, String rssi, String antNo,String deviceNo, String direction) {

			if( ! last_epc_processed.equals(data)) {
				
				//ACTUALIZA ULTIMO PROCESADO
				System.out.println("Nuevo EPC detectado:".concat(data));
				last_epc_processed = data; 
				
				// TIME 
				String right_now_time = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss").format(new Date());
				
				//SERVER SEND 
				ApiServer.enviarLectura(last_epc_processed, right_now_time); 
				
				//GUARDA EN LOG LOCAL
				log_file.guardarLectura(last_epc_processed, right_now_time);
				

			}
		}
	}

}



