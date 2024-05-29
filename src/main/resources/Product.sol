// SPDX-License-Identifier: GPL-3.0

pragma solidity >=0.6.1 <=0.7.6;
//pragma experimental ABIEncoderV2;

contract Product {
    address payable private owner;
    address public productSaleContractAddress;
    uint256 private startTime;
    uint256 private productId;
    uint256[] productInfoList;

    event ReturnValue(string productOwnerName, string productName, uint creationDate);
    // Event triggered for completed order, delivery and transfer of ownership
    event OwnershipTransferred(string indexed indexedProductOwnerName, uint indexed productId, string productOwnerName);

    struct ProductInformation {
        string productOwnerName;
        string productName;
        uint creationDate;
    }

    mapping(uint256 => ProductInformation) productInfos;

    modifier onlyWhileOpen() {
        require(block.timestamp >= startTime);
        _;
    }

    modifier onlyOwner () {
        require(msg.sender == owner);
        _;
    }

    constructor() public {
        owner = msg.sender;
        //startTime = now;
        startTime = block.timestamp;
        productId = 0;

        // Solidity 0.6
        // casting from address payable to address
        //address payable _addr = msg.sender;
        //address addr = address(_addr);

        // Solidity 0.5
        // casting from address to address payable
        //address _addr = msg.sender;
        //address payable addr = address(uint160(_addr));
    }

    /*
     * onlyOwner is custom modifier
     */
    function close() public onlyOwner {
        selfdestruct(owner);
        // `owner` is the owners address
    }

    function setProductSaleContractAddress(address contractAddress) public onlyOwner {
        productSaleContractAddress = contractAddress;
    }

    function getProductSaleContractAddress() public view onlyOwner returns (address) {
        return productSaleContractAddress;
    }

    function addProduct(string memory _productOwnerName, string memory _productName) public onlyWhileOpen {
        require(msg.sender == owner || msg.sender == productSaleContractAddress);
        Product.ProductInformation storage productInfo = productInfos[productId + 1];

        productInfo.productOwnerName = _productOwnerName;
        productInfo.productName = _productName;
        //productInfo.creationDate = now;
        productInfo.creationDate = block.timestamp;

        productInfoList.push(productId + 1);
        productId = productId + 1;

        // Trigger the event OwnershipTransferred
        emit OwnershipTransferred(_productOwnerName, productId, _productOwnerName);
    }

    function getAllProductIds() public view onlyOwner returns (uint256[] memory){
        return productInfoList;
    }

    function getProductFromProductId(uint256 _productId) public view returns (string memory ownerName, string memory productName, uint creationDate){
        return (productInfos[_productId].productOwnerName,
        productInfos[_productId].productName,
        productInfos[_productId].creationDate);
    }

    function getProductCount() public view returns (uint productCount) {
        return productInfoList.length;
    }

    function isProduct(uint256 _productId, string memory _productName) public view returns (bool isIndeed) {
        if (keccak256(abi.encodePacked(productInfos[_productId].productName)) == keccak256(abi.encodePacked(_productName))) {
            return true;
        }
        return false;
    }
}