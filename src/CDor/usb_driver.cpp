// usb_driver.cpp
#include <iostream>
#include <string>
#include <cstdio>
#include <cstring>
#include "libusb.h" 

extern "C" {
    struct DeviceStatus {
        int statusCode;
        char message[256];
    };

    // The Handle for the physical device
    libusb_device_handle *dev_handle = NULL;
    unsigned char bulk_out_endpoint = 0;
    unsigned char bulk_in_endpoint = 0;
    int claimed_interface = -1;
    libusb_context *ctx = NULL;

    __declspec(dllexport) int connect_physical_device(int vid, int pid, int interface_idx) {
        // Reset globals just in case
        bulk_out_endpoint = 0;
        bulk_in_endpoint = 0;
        claimed_interface = -1;

        // 1. Initialize the Library (if not already)
        if (ctx == NULL) {
            int r = libusb_init(&ctx);
            if(r < 0) {
                std::cout << "[C++ Real] Init Error: " << r << std::endl;
                return -1;
            }
        }

        // 2. Open the Device physically
        dev_handle = libusb_open_device_with_vid_pid(ctx, vid, pid);

        if(dev_handle == NULL) {
            std::cout << "[C++ Real] device not found (or driver blocks access)." << std::endl;
            return 0; // Fail
        }

        std::cout << "[C++ Real] Checking Interface " << interface_idx << "..." << std::endl;

        // 3. Inspect the SPECIFIC Interface
        libusb_device *dev = libusb_get_device(dev_handle);
        libusb_config_descriptor *config;
        libusb_get_active_config_descriptor(dev, &config);

        if (interface_idx >= config->bNumInterfaces) {
            std::cout << "[C++ Real] Interface " << interface_idx << " does not exist." << std::endl;
            libusb_close(dev_handle);
            dev_handle = NULL;
            return 0;
        }

        const libusb_interface_descriptor *inter = &config->interface[interface_idx].altsetting[0];
        
        // Scan Endpoints ONLY for this interface
        for(int j=0; j<inter->bNumEndpoints; j++) {
            const libusb_endpoint_descriptor *ep = &inter->endpoint[j];
            
            if((ep->bEndpointAddress & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN) {
                 if((ep->bmAttributes & LIBUSB_TRANSFER_TYPE_MASK) == LIBUSB_TRANSFER_TYPE_BULK) {
                    if (bulk_in_endpoint == 0) bulk_in_endpoint = ep->bEndpointAddress;
                }
            } else {
                if((ep->bmAttributes & LIBUSB_TRANSFER_TYPE_MASK) == LIBUSB_TRANSFER_TYPE_BULK) {
                    if (bulk_out_endpoint == 0) bulk_out_endpoint = ep->bEndpointAddress;
                }
            }
        }

        std::cout << "   -> Endpoints found: IN=0x" << std::hex << (int)bulk_in_endpoint << " OUT=0x" << (int)bulk_out_endpoint << std::endl;

        if (bulk_out_endpoint == 0 || bulk_in_endpoint == 0) {
            std::cout << "[C++ Real] Interface " << interface_idx << " is missing required BULK endpoints." << std::endl;
            libusb_close(dev_handle);
            dev_handle = NULL;
            return 0;
        }

        // 4. Claim ONLY this Interface
        if(libusb_claim_interface(dev_handle, interface_idx) < 0) {
            std::cout << "[C++ Real] Failed to claim Interface " << interface_idx << "." << std::endl;
            libusb_close(dev_handle);
            dev_handle = NULL;
            return 0;
        }

        claimed_interface = interface_idx;
        std::cout << "[C++ Real] Interface " << interface_idx << " Claimed Successfully." << std::endl;
        return 1; // Success
    }
    
    __declspec(dllexport) void close_device() {
        if (dev_handle != NULL) {
            if (claimed_interface != -1) {
                libusb_release_interface(dev_handle, claimed_interface);
            }
            libusb_close(dev_handle);
            dev_handle = NULL;
        }
        // Don't kill ctx, we might want to reconnect
        bulk_in_endpoint = 0;
        bulk_out_endpoint = 0;
        claimed_interface = -1;
        std::cout << "[C++ Real] Device Closed." << std::endl;
    }

    __declspec(dllexport) void send_raw_packet(unsigned char* data, int length, DeviceStatus* status) {
        if (dev_handle == NULL) {
            status->statusCode = 500;
            strcpy(status->message, "No hardware connected.");
            return;
        }
        
        if (bulk_out_endpoint == 0) {
             status->statusCode = 500;
             strcpy(status->message, "No BULK OUT Endpoint found!");
             return;
        }

        std::cout << "[C++ Real] Sending " << length << " bytes to Endpoint 0x" << std::hex << (int)bulk_out_endpoint << "..." << std::endl;

        // 4. The Real Write
        int actual_written;
        // Output timeout increased to 2000ms
        int r = libusb_bulk_transfer(dev_handle, bulk_out_endpoint, data, length, &actual_written, 2000);

        if(r == 0 && actual_written == length) {
            status->statusCode = 0; // Success
            strcpy(status->message, "Packet sent to physical hardware!");
            std::cout << "[C++ Real] Write Success!" << std::endl;
        } else {
            status->statusCode = r; // Error Code
            sprintf(status->message, "Write Failed. Error: %d", r);
            std::cout << "[C++ Real] Write Failed (Error Code: " << r << ")" << std::endl;
        }
    }

    __declspec(dllexport) int read_data(unsigned char* buffer, int maxLength, DeviceStatus* status) {
        if (dev_handle == NULL) {
             status->statusCode = -1;
             strcpy(status->message, "No hardware connected.");
             return 0;
        }

        if (bulk_in_endpoint == 0) {
             status->statusCode = -2;
             strcpy(status->message, "No BULK IN Endpoint found!");
             return 0;
        }

        std::cout << "[C++ Real] Reading up to " << maxLength << " bytes from Endpoint 0x" << std::hex << (int)bulk_in_endpoint << "..." << std::endl;

        int actual_read = 0;
        int r = libusb_bulk_transfer(dev_handle, bulk_in_endpoint, buffer, maxLength, &actual_read, 3000); // 3s Timeout

        if (r == 0) {
            status->statusCode = 0; // Success
            sprintf(status->message, "Read %d bytes successfully", actual_read);
            std::cout << "[C++ Real] Read Success! (" << actual_read << " bytes)" << std::endl;
            return actual_read;
        } else {
             // It is common to time out if device has nothing to say
             status->statusCode = r;
             sprintf(status->message, "Read Failed/Timeout. Error: %d", r);
             std::cout << "[C++ Real] Read Failed/Timeout (Error Code: " << r << ")" << std::endl;
             return 0;
        }
    }
}