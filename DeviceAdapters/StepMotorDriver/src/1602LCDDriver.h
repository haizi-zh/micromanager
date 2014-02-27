#include <reg52.h>   
#include <intrins.h>
#ifndef LCD_1602_2010_09_02
#define LCD_1602_2010_09_02


sbit LCDRS = P2^3;                    
sbit LCDRW = P2^4;                        
sbit LCDEn = P2^5;   

#define DBPort  P0    

#define LCD_Command  0
#define LCD_Data     1

#define LCD_CLOSE          0x08     
#define LCD_CLEAR_SCREEN   0x01   


#define LCD_SHOW_ON   0x0c    
#define LCD_SHOW_OFF  0x08     

#define LCD_CURSOR_0N  0x0a   
#define LCD_CURSOR_OFF  0x08         

#define LCD_CURSOR_FLASH_ON   0x09     
#define LCD_CURSOR_FLASH_OFF  0x08
void LCD_Initial();                    
void LCD_Write(bit bRS, unsigned char ucPort);             
void LCD_CoordinateXY(unsigned char X, unsigned char Y);   
void LCD_Printf00(unsigned char *ucStr); 
void LCD_Printf1(unsigned char *ucStr); 
void LCD_Printf2(unsigned char *ucStr);        
void LCD_DelayTime(unsigned char ucCount);     
//void LCD_BusyCheck();           
#endif
