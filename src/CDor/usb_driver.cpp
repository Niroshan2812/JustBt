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

    __declspec(dllexport) int connect_physical_device(int vid, int pid) {
        // 1. Initialize the Library
        libusb_context *ctx = NULL;
        int r = libusb_init(&ctx);
        if(r < 0) {
            std::cout << "[C++ Real] Init Error: " << r << std::endl;
            return -1;
        }

        // 2. Open the Device physically
        dev_handle = libusb_open_device_with_vid_pid(ctx, vid, pid);

        if(dev_handle == NULL) {
            std::cout << "[C++ Real] device not found (or driver blocks access)." << std::endl;
            return 0; // Fail
        }

        std::cout << "[C++ Real] HARDWARE LINK ESTABLISHED (VID: " << std::hex << vid << " PID: " << pid << ")" << std::endl;

        // 3. Auto-Detect Endpoints AND Interface
        libusb_device *dev = libusb_get_device(dev_handle);
        libusb_config_descriptor *config;
        libusb_get_active_config_descriptor(dev, &config);

        int target_interface = -1;

        std::cout << "[C++ Real] Scanning Endpoints..." << std::endl;
        for(int i=0; i<config->bNumInterfaces; i++) {
            const libusb_interface_descriptor *inter = &config->interface[i].altsetting[0];
            for(int j=0; j<inter->bNumEndpoints; j++) {
                const libusb_endpoint_descriptor *ep = &inter->endpoint[j];
                std::cout << "   -> Interface " << i << " | Endpoint: 0x" << std::hex << (int)ep->bEndpointAddress;
                
                if((ep->bEndpointAddress & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN) {
                    std::cout << " (IN/READ)";
                     if((ep->bmAttributes & LIBUSB_TRANSFER_TYPE_MASK) == LIBUSB_TRANSFER_TYPE_BULK) {
                        std::cout << " [BULK] <- FOUND IN";
                        if (bulk_in_endpoint == 0) {
                             bulk_in_endpoint = ep->bEndpointAddress;
                        }
                    }
                } else {
                    std::cout << " (OUT/WRITE)";
                    if((ep->bmAttributes & LIBUSB_TRANSFER_TYPE_MASK) == LIBUSB_TRANSFER_TYPE_BULK) {
                        std::cout << " [BULK] <- FOUND OUT";
                        if (bulk_out_endpoint == 0) { // Take the first one we find
                            bulk_out_endpoint = ep->bEndpointAddress;
                            // We assume IN/OUT are on the same interface usually
                            target_interface = i;
                        }
                    }
                }
                std::cout << std::endl;
            }
        }

        if (bulk_out_endpoint == 0 || target_interface == -1) {
            std::cout << "[C++ Real] Could not find any BULK OUT endpoint." << std::endl;
            return 0;
        }

        std::cout << "[C++ Real] Target Interface: " << target_interface << " | Out Endpoint: 0x" << std::hex << (int)bulk_out_endpoint << " | In Endpoint: 0x" << (int)bulk_in_endpoint << std::endl;

        // 4. Claim the SPECIFIC Interface
        if(libusb_claim_interface(dev_handle, target_interface) < 0) {
            std::cout << "[C++ Real] Failed to claim Interface " << target_interface << "." << std::endl;
            return 0;
        }

        std::cout << "[C++ Real] Interface " << target_interface << " Claimed Successfully." << std::endl;
        return 1; // Success
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
            status->statusCode = 200;
            strcpy(status->message, "Packet sent to physical hardware!");
            std::cout << "[C++ Real] Write Success!" << std::endl;
        } else {
            status->statusCode = 400;
            sprintf(status->message, "Write Failed. Error: %d", r);
            std::cout << "[C++ Real] Write Failed (Error Code: " << r << ")" << std::endl;
        }
    }

    __declspec(dllexport) int read_data(unsigned char* buffer, int maxLength, DeviceStatus* status) {
        if (dev_handle == NULL) {
             status->statusCode = 500;
             strcpy(status->message, "No hardware connected.");
             return 0;
        }

        if (bulk_in_endpoint == 0) {
             status->statusCode = 500;
             strcpy(status->message, "No BULK IN Endpoint found!");
             return 0;
        }

        std::cout << "[C++ Real] Reading up to " << maxLength << " bytes from Endpoint 0x" << std::hex << (int)bulk_in_endpoint << "..." << std::endl;

        int actual_read = 0;
        int r = libusb_bulk_transfer(dev_handle, bulk_in_endpoint, buffer, maxLength, &actual_read, 3000); // 3s Timeout

        if (r == 0) {
            status->statusCode = 200;
            sprintf(status->message, "Read %d bytes successfully", actual_read);
            std::cout << "[C++ Real] Read Success! (" << actual_read << " bytes)" << std::endl;
            return actual_read;
        } else {
             // It is common to time out if device has nothing to say
             status->statusCode = (r == LIBUSB_ERROR_TIMEOUT) ? 204 : 400;
             sprintf(status->message, "Read Failed/Timeout. Error: %d", r);
             std::cout << "[C++ Real] Read Failed/Timeout (Error Code: " << r << ")" << std::endl;
             return 0;
        }
    }
}